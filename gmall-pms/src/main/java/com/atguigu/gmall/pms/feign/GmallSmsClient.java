package com.atguigu.gmall.pms.feign;


import com.atguigu.gmall.sms.api.GmallSmsApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("sms-service")
public interface GmallSmsClient extends GmallSmsApi {
    //继承了sms-interface中的vo接口 不用在这写远程调用的方法
    // 也不需要在该模块创建SkuSalesVo类  直接引用继承的接口中的方法和类
    // openfeign 最佳实践  防止sms中内容修改后 pms远程调用他但是不知道 引发错误

}
