After installing the plugin,you'll get a new Build Step entry named "Aliyun Container Service Deploy". Fill the configuration and it will deploy the docker-compose configuration when the pre build step build successfully. 

#### Parameters
###### MasterUrl 
MasterUrl is the endpoint of a container cluster.you can find the value on the console of  <a href="https://cs.console.aliyun.com/#/cluster" target="_blank"/>Aliyun Container Service</a>.

<img src="/picture/ded60b9589c65187ed6b1d8965bbae96.png" width=80% style="max-width=1000px"/>
<img src="/picture/4446ae8b09a2a4a04e9488fdd4f07059.png" width=80% style="max-width=1000px"/>     

###### ApplicationName 
Application is a logical concept in Aliyun Container Service.Application is the mininum unit of a single releasement.A standard application consist of several services and a service consist of serveral containers.If you want to deploy a application to Aliyun Container Service. You need to come up a awesome name firstly 😊.
###### ComposeTemplate 
ComposeTemplate is the configuration of the application. The blank requires the relative path of the docker-compose.If your docker-compose.yml is under the path of docker/docker-compose.yml.Just fill the blank with the path.
###### HostCredetials 
HostCredetials are the certs of the target cluster. You can visit the remote container cluster with the specific certs on any computer with a docker client.

<img src="/picture/15e3bddcde04166a6b41843cab7642ec.png" width=80% style="max-width=1000px"/>         
open the pem files , extract the content and fill the blank below.
<img src="/picture/04d625cf2e2a982376e94768fca74755.png" width=80% style="max-width=1000px"/>
