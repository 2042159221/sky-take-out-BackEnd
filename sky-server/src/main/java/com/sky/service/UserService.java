package com.sky.service;

import com.sky.entity.User;
import com.sky.dto.UserLoginDTO;
/**
 * 微信登陆
 * @param userLoginDTO
 * @return
 */
public interface UserService {

    User wxLogin(UserLoginDTO userLoginDTO);
}
