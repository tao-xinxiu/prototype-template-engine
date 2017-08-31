# prototype-template-engine
The project is the prototype of an Architecture-based Framework for automating the update of multiple microservices on multiple distributed PaaS platforms (ex. Cloud Foundry, Heroku).

## Usage
### start server
```
git clone https://github.com/tao-xinxiu/prototype-template-engine.git
cd prototype-template-engine/
mvn install
java -jar target/prototype-template-engine-0.0.1-SNAPSHOT.jar
```

### client
The client could setup a pipeline (with tools as [Jenkins](https://jenkins.io/) or [Concourse](https://concourse.ci/)) as [example](https://gitlab.com/x_tao/microservices-demo-deployment), or write a simple script as [example](https://gitlab.com/x_tao/experiment/blob/master/scripts/update.sh) to send the update request to the server. The basic idea is demonstrated in following pseudo-code:
```
set_strategy_config(strategy_name, strategy_config)

while(!is_instantiation(final_architecture))
    next_architecture = next(final_architecture)
    push(next_architecture)
done
```

## model
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
