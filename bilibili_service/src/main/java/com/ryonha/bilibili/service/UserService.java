package com.ryonha.bilibili.service;

import com.ryonha.bilibili.dao.UserDao;
import com.ryonha.bilibili.domain.User;
import com.ryonha.bilibili.domain.UserInfo;
import com.ryonha.bilibili.domain.constant.UserConstant;
import com.ryonha.bilibili.domain.exception.ConditionException;
import com.ryonha.bilibili.service.util.MD5Util;
import com.ryonha.bilibili.service.util.RSAUtil;
import com.ryonha.bilibili.service.util.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


import java.util.Date;

@Service
public class UserService {
    @Autowired     
    private UserDao userDao;

     public void addUser(User user) {
         String phone = user.getPhone();
         if (StringUtils.isEmpty(phone)) {
            throw new ConditionException("手机号不能为空！");
         }
         Date now = new Date();
         String salt = String.valueOf(now.getTime());
         String password = user.getPassword();
         String rawPassword;
         try {
             rawPassword = RSAUtil.decrypt(password);
         }catch (Exception e) {
             throw new ConditionException("密码解密失败！");
         }
         String md5Password = MD5Util.sign(rawPassword, salt, "UTF-8");
         user.setSalt(salt);
         user.setPassword(md5Password);
         user.setCreateTime(now);
         userDao.addUser(user);
         //添加用户信息
         UserInfo userInfo = new UserInfo();
         userInfo.setUserId(user.getId());
         userInfo.setNick(UserConstant.DEFAULT_NICK);
         userInfo.setBirth(UserConstant.DEFAULT_BIRTH);
         userInfo.setGender(UserConstant.GENDER_MALE);
         userInfo.setCreateTime(now);
         userDao.addUserInfo(userInfo);
     }

     public User getUserByPhone(String phone){
         return userDao.getUserByPhone(phone);
     }

    public String login(User user) throws Exception{
        String phone = user.getPhone();
        if (StringUtils.isEmpty(phone)) {
            throw new ConditionException("手机号不能为空！");
        }
        User dbuser = this.getUserByPhone(phone);
        if (dbuser == null) {
            throw new ConditionException("当前用户不存在！");
        }
        String password = user.getPassword();
        String rawPassword;
        try {
            rawPassword = RSAUtil.decrypt(password);
        } catch (Exception e) {
            throw new ConditionException("密码解密失败！");
        }
        String salt = dbuser.getSalt();
        String md5Password = MD5Util.sign(rawPassword, salt, "UTF-8");
        if (!md5Password.equals(dbuser.getPassword())) {
            throw new ConditionException("密码错误！");
        }
        return TokenUtil.generateToken(dbuser.getId());
    }

    public User getUserInfo(Long userId) {
        User user= userDao.getUserById(userId);
        UserInfo userInfo = userDao.getUserInfoByUserId(userId);
        user.setUserInfo(userInfo);
        return user;
    }

    public void updateUsers(User user) throws Exception{
        Long id = user.getId();
        User dbUser = userDao.getUserById(id);
        if (dbUser == null) {
            throw new ConditionException("用户不存在！");
        }
        if (!StringUtils.isEmpty(user.getPassword())) {
            String rawPassword = RSAUtil.decrypt(user.getPassword());
            String md5Password = MD5Util.sign(rawPassword, dbUser.getSalt(), "UTF-8");
        }
        user.setUpdateTime(new Date());
        userDao.updateUsers(user);
    }

    public void updateUserInfos(UserInfo userInfo) {
         userInfo.setUpdateTime(new Date());
         userDao.updateUserInfos(userInfo);
    }
}
