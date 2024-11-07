package com.hmdp.utils;


/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface ILock  {
boolean lock(long timeout);
void unlock( );
}
