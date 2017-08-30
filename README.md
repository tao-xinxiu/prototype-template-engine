# prototype-template-engine
an Architecture-based Automated Update Framework for Distributed Microservices on PaaS

## Usage
### start server
```
git clone https://github.com/tao-xinxiu/prototype-template-engine.git
cd prototype-template-engine/
mvn install
java -jar target/prototype-template-engine-0.0.1-SNAPSHOT.jar
```

### client
The client could setup a pipeline (with tools as [Jenkins](https://jenkins.io/) or [Concourse](https://concourse.ci/)) as [example](https://gitlab.com/x_tao/microservices-demo-deployment), or write a simple script as [example](https://gitlab.com/x_tao/experiment/blob/master/scripts/update.sh) to send the update request to the server.

## API
### pull current architecture
request: POST /pull

body: Collection<[PaaSSite](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/model/PaaSSite.java)> managingSites 

### push a desired architecture
POST /push

body: [Overview](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/model/state/Overview.java) desiredArchitecture

### calculate next desired architecture
POST /next

body [Overview](https://github.com/tao-xinxiu/prototype-template-engine/blob/master/src/main/java/com/orange/model/state/Overview.java) finalArchitecture
