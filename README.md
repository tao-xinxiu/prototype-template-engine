# prototype-template-engine
The project is the prototype of an Architecture-based Framework for automating the update of multiple microservices running on multiple distributed PaaS platforms (e.g., Cloud Foundry, Heroku). The framework supports various updating strategies (e.g., BlueGreen, Canary, CleanRedeploy, etc.), and greatly facilitates fixing failures during updating.

## Usage
### compile the framework
```
git clone https://github.com/tao-xinxiu/prototype-template-engine.git
cd prototype-template-engine/
mvn install
alias ms-update="java -jar $PWD/target/prototype-template-engine-0.0.1-SNAPSHOT-shaded.jar"
```
Now you are prepared to execute the commands of our framework.

### conduct the update
You have two ways to process an update:
1) Auto mode: directly process the `update` command to automatically deliver a target architecture.
2) Step by step mode: to process an update step by step, you can invoke manually `next` and `push` commands, so that you will always preview the next architecture before delivering it. This usage mode is often used during the implementation and testing of new custom strategy.

## Model
An [architecture](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/model/architecture/Architecture.java) is composed by the multiple PaaS sites, each site contains a set of [microservices architecture](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/model/architecture/Microservice.java).

## CLI
### deliver a desired architecture with a strategy
The command delivers a target architecture with a specified strategy.  

Command: `ms-update update -a $FINAL_ARCHI -sn $STRATEGY -sc $STRATEGY_CONFIG -oc $OP_CONFIG`

Parameters: 
- `FINAL_ARCHI`: a json file describing the structure [Architecture](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/model/architecture/Architecture.java)
- `STRATEGY`: String, the name of the chosen strategy
- `STRATEGY_CONFIG`: a json file describing the structure [StrategyConfig](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/model/StrategyConfig.java)
- `OPCONFIG_FILE`: a json file with the structure [OperationConfig](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/model/OperationConfig.java)

### pull current architecture
The command gets the current architecture for all managed PaaS sites. 

Command: `ms-update pull -s $SITES_FILE -oc $OPCONFIG_FILE`

Parameters: 
- `SITES_FILE`: a json file describing the structure Collection<[PaaSSiteAccess](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/model/PaaSSiteAccess.java)> 
- `OPCONFIG_FILE`: a json file describing the structure [OperationConfig](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/model/OperationConfig.java)

### push a desired architecture
The command evolves the specified sites from their current architecture to the desired target architecture in the most direct way without considering update strategies.  

Command: `ms-update push -a $DESIRED_ARCHI -oc $OPCONFIG_FILE`

Parameters: 
- `DESIRED_ARCHI`: a json file describing the structure [Architecture](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/model/architecture/Architecture.java)
- `OPCONFIG_FILE`: a json file describing the structure [OperationConfig](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/model/OperationConfig.java)

### calculate next desired architecture
The command computes the next desired architecture based on the chosen strategy.  

Command: `ms-update next -a $FINAL_ARCHI -sn $STRATEGY -sc $STRATEGY_CONFIG -oc $OP_CONFIG`

Parameters: 
- `FINAL_ARCHI`: a json file describing the structure [Architecture](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/model/architecture/Architecture.java)
- `STRATEGY`: String, the name of the chosen strategy
- `STRATEGY_CONFIG`: a json file describing the structure [StrategyConfig](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/model/StrategyConfig.java)
- `OPCONFIG_FILE`: a json file describing the structure [OperationConfig](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/model/OperationConfig.java)

### verify current architecture
The command checks whether the current architecture is the instantiation of the desired target architecture  

Command: `ms-update arrived -a $DESIRED_ARCHI -oc $OPCONFIG_FILE`

Parameters: 
- `DESIRED_ARCHI`: a json file describing the structure [Architecture](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/model/architecture/Architecture.java)
- `OPCONFIG_FILE`: a json file describing the structure [OperationConfig](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/model/OperationConfig.java)

## Robustness
This framework provides a kill-continue capability. Whenever the update process is stopped, either voluntarily by the user or involuntarily due to a failure, the user can always re-start it by re-invoking the  `update` [script](#client). In the practise, the user can configure a `retry` pattern in the pipeline setup. To fix failures caused by microservice implementation or configuration, the user can change the desired microservice architecture `final_architecture`. In addition, the user can also change the chosen `strategy`.

## Updating Strategy
In the framework, the user controls the updating process by choosing a provided or custom strategy. The choice of strategy depend's on the microservice architecture constraints and non-functional requirements (availability, resource usage, or updating duration etc.)

### provided strategy
The following strategies are provided:
- [BlueGreen](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/strategy/impl/BlueGreenStrategy.java)
- [Canary](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/strategy/impl/CanaryStrategy.java)
- [BlueGreen Canary Mix](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/strategy/impl/BlueGreenCanaryMixStrategy.java)
- [BlueGreen by group](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/strategy/impl/BlueGreenGroupStrategy.java)
- [Straight](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/strategy/impl/StraightStrategy.java)
- [Inplace update with test](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/strategy/impl/InplaceTestStrategy.java)
- [Deploy](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/strategy/impl/DeployStrategy.java)
- [Clean up then redeploy](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/strategy/impl/CleanRedeployStrategy.java)
- [Add then Remove](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/strategy/impl/AddRemoveStrategy.java)
- [Remove then Add](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/strategy/impl/RemoveAddStrategy.java)

To process an update site by site, you can simply specify `parallelAllSites` as `false` and set `sitesOrder` as the order of sites to update (e.x., `[["site1"], ["site2", "site3"], ["site4", "site5"]]`)

To choose among several strategies, you can consider the following table that compares them. It shows the influence of applying different strategies for updating the code of a microservice.

| strategy | multi-version coexisted | duration | resource consumption | performance degradation | available instances |
|-----------|-----|-----|---------|-----|---------|
| BlueGreen | Yes | ++  | N ~ 2N  | 0   | N ~ 2N  |
| Canary    | Yes | +++ | N-1 ~ N | +   | N-1 ~ N |
| Mix       | Yes | ++  | N ~ N+1 | 0   | N ~ N+1 |
| Inplace   | No  | +   | N       | +++ | 0 ~ N   |
| CleanRedeploy | No | ++ | 0 ~ N | ++++ | 0 ~ N  |

In the table, the property `resource consumption` reflects the number of microservice instances. `N` stands for the desired number of instances of the microservice. The property `available instances` corresponds to the number of instances in running state. It is used to demonstrate more clearly `performance degradation`, as more instances available usually means less performance degradation.

### custom strategy
The user can implement its proper strategy by implementing the [Strategy interface](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/strategy/Strategy.java). The key of the implementation of a strategy is to specify a sequence of [transitions](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/strategy/Transition.java). The `transitions` defined in [strategy library](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/strategy/StrategyLibrary.java) can be used to compose new strategies.

## Evaluation
An evaluation of this prototype is available in [another repo](https://gitlab.com/xxtao/experiment).
