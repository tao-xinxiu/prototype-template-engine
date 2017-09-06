# prototype-template-engine
The project is the prototype of an Architecture-based Framework for automating the update of multiple microservices on multiple distributed PaaS platforms (ex. Cloud Foundry, Heroku). The framework supports multiple updating strategies (ex. BlueGreen, Canary, CleanRedeploy etc.), and greatly facilitates fixing failures during updating.

## Usage
### start server
```
git clone https://github.com/tao-xinxiu/prototype-template-engine.git
cd prototype-template-engine/
mvn install
java -jar target/prototype-template-engine-0.0.1-SNAPSHOT.jar
```

### client
The client could setup a pipeline (with tools as [Jenkins](https://jenkins.io/) or [Concourse](https://concourse.ci/)) as [example](https://gitlab.com/x_tao/microservices-demo-deployment), or write a simple script as [example](https://gitlab.com/x_tao/experiment/blob/master/scripts/update.sh) to send the update request to the server. The basic idea is demonstrated in following `update` script:
```
set_strategy_config(strategy_name, strategy_config)

while(!is_instantiation(final_architecture))
    next_architecture = next(final_architecture)
    push(next_architecture)
done
```
To perform a more prudent updating process, the user could invoke manually `next` and `push` command, so that it could always preview the next architecture before delivering it. This usage mode is often used during the implementation and testing of new custom strategy.

## Model
An [architecture](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/model/state/Architecture.java) is composed by the multiple PaaS sites, each site contains a set of [microservices architecture](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/model/state/ArchitectureMicroservice.java).

## API
### pull current architecture
The endpoint get the current architecture of all managing PaaS sites.  
request: PUT /pull  
body: Collection<[PaaSSite](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/model/PaaSSite.java)> managingSites 

### push a desired architecture
The endpoint evolve the specified sites from their current architecture to the desired architecture.  
request: POST /push  
body: [Architecture](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/model/state/Architecture.java) desiredArchitecture

### calculate next desired architecture
The endpoint calculate the next desired architecture based on configured strategy.  
request: POST /next  
body: [Architecture](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/model/state/Architecture.java) finalArchitecture

### verify current architecture
The endpoint verify whether the current architecture is the instantiation of a desired architecture  
request: POST /is_instantiation  
body:  [Architecture](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/model/state/Architecture.java) desiredArchitecture 

### set strategy configuration
request: PUT /set_strategy_config  
parameter: strategy name  
body: [StrategyConfig](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/model/StrategyConfig.java)

### set operation configuration
request: PUT /set_operation_config  
body: [OperationConfig](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/model/OperationConfig.java)

## Robustness
This framework provides the kill-continue capability. That is, whenever the update process is stopped, either voluntarily by the user or involuntarily due to a failure, user could always re-start it by re-invoking the demonstrated  `update` script. In the practise, the user could easily configure `retry` in the pipeline setup or use loop in the script to avoid temporary failures (ex. network error). To correct the failure caused by microservice implementation or configuration, the user could change the desired microservice architecture `final_architecture`. In addition, the user could also change the chosen `strategy` to correct the erroneous strategy implementation.

## Updating Strategy
In the framework, user control the updating process by choosing a provided or custom strategy. The choice of strategy is depending on the microservice architecture constraints and non-functional requirement (availability, resource usage, or updating duration etc.)

### provided strategy
The following strategies is provided:
- [BlueGreen](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/nextstate/strategy/BlueGreenStrategy.java)
- [Canary](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/nextstate/strategy/CanaryStrategy.java)
- [BlueGreen Canary Mix](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/nextstate/strategy/BlueGreenCanaryMixStrategy.java)
- [Inplace](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/nextstate/strategy/InplaceStrategy.java)
- [Inplace with test](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/nextstate/strategy/InplaceTestStrategy.java)
- [Clean up then redeploy](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/nextstate/strategy/CleanRedeployStrategy.java)
- [Add then Remove](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/nextstate/strategy/AddRemoveStrategy.java)
- [Remove then Add](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/nextstate/strategy/RemoveAddStrategy.java)

### custom strategy
The user could implement its proper strategy by implement [Strategy interface](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/nextstate/strategy/Strategy.java). The key of the implementation of a strategy is to specify a sequence of [transitions](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/nextstate/strategy/Transit.java). The `transitions` defined in [strategy library](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/nextstate/strategy/StrategyLibrary.java) could be used to compose a new strategy.
