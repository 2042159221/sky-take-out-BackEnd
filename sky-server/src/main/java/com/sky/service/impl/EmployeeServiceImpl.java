package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;

@Service
@Slf4j
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    @Override
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        log.info("登录用户名: {}, 密码: {}", username, password);

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);
        log.info("查询到的员工信息: {}", employee);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            log.error("账号不存在: {}", username);
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        // 进行md5加密，然后再进行比对
        String encryptedPassword = DigestUtils.md5DigestAsHex(password.getBytes());
        log.info("输入密码: {}, 加密后: {}, 数据库密码: {}", password, encryptedPassword, employee.getPassword());
        
        if (!encryptedPassword.equals(employee.getPassword())) {
            //密码错误
            log.error("密码错误, 输入密码加密后: {}, 数据库密码: {}", encryptedPassword, employee.getPassword());
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (StatusConstant.DISABLE.equals(employee.getStatus())) {
            //账号被锁定
            log.error("账号已锁定: {}", username);
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        log.info("登录成功, 返回员工信息: {}", employee);
        return employee;
    }


    /**
     * 新增员工
     *
     * @param employeeDTO
     */
    @Override
    public void save(EmployeeDTO employeeDTO) {
        Employee employee =  new Employee();

        //对象属性拷贝
        BeanUtils.copyProperties(employeeDTO,employee);

        //设置账号的状态，默认正常状态1，0表示锁定
        employee.setStatus(StatusConstant.ENABLE);

        // set password default :123456
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));

        //设置当前记录的创建时间和修改时间
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());

        // 从ThreadLocal或其他地方获取当前登录用户的ID
        Long currentId = BaseContext.getCurrentId();
        employee.setCreateUser(currentId);
        employee.setUpdateUser(currentId);

        employeeMapper.insert(employee);
    }

}
