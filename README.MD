## Jenkins插件设计   
### 概述  
从代码到容器服务目前缺少了很重要的一环，就是自动发布应用到容器服务的流程。   

### 大致的设计与步骤  
我们大致会按照三个阶段完成jenkins的开发      
1.完成基本的jenkins的流程，从compose到容器服务的过程，只支持rolling-update        
2.支持扩展的方式使用更多的API，利用配置文件读取更多约定的配置，并支持动态slave           
3.支持持续集成的历史与记录，支持回滚等策略        

### 流程图   
<img src="http://moyuan.oss-cn-beijing.aliyuncs.com/jenkins/6912376489ca1e2c6d68aaf9c25cdd4c.png" width=600px/>

<img src="http://moyuan.oss-cn-beijing.aliyuncs.com/jenkins/e2da11c53ea1d9d4d798f500d0242209.png"/>

### 具体的实现
扩展builder的扩展点，将容器服务相关的部分单独封装，在第二个阶段使用的动态slave会调用同一个位置。    


### 资料与内容
可扩展的挂载点： https://wiki.jenkins-ci.org/display/JENKINS/Extension+points    

项目地址：http://gitlab.alibaba-inc.com/cos/jenkins-cos/tree/master    


## 开发调试插件   
mvn hpi:run 
