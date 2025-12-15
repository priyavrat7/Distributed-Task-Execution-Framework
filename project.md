# Distributed Task Execution Framework

## Overview

This project implements a simple distributed computing platform following a **master-worker(s) architecture** that uses **ZooKeeper for coordination**. The high-level objective of the platform is that a client can submit a "task/job" for computation to the platform, which will execute the task/job using one of its many workers and provide the client with the result. The project includes template code with some functionalities already implemented. For the purpose of this project, crashes and failovers of the member processes are not considered, and it is assumed that all processes gracefully terminate.

**Prerequisites:** This project requires familiarity with ZooKeeper concepts, the ZooKeeper tutorial, and the ZookeeperSetup-Distributed documentation. A ZooKeeper ensemble must be set up and tested before running the system.

## One-time Setup of ZK Nodes

Using the ZooKeeper client CLI (`zkCli.sh`), create permanent znodes:
- `/distXX` - where `XX` is the group/namespace identifier (e.g., `/dist04`)
- `/distXX/tasks` - for task submissions
- `/distXX/workers` - for worker registration

Additional permanent znodes may be created as needed based on the design.

## Project Structure

The project includes a substantial amount of template code organized in the following structure:

```
zk
|-- clnt
|   |-- compileclnt.sh
|   |-- DistClient.java
|   -- runclnt.sh
|-- dist
|   |-- compilesrvr.sh
|   |-- DistProcess.java
|   -- runsrvr.sh
-- task
|   |-- compiletask.sh
|   |-- DistTask.java
|   -- MCPi.java
```

**Setup Steps:**
- Edit the `*.java` and `*.sh` files to replace `XX` with the appropriate group/namespace identifier
- Update the ZK server names and client ports to match your ZooKeeper ensemble configuration
- Compile the `task`, `clnt`, `dist` directories in that order
- Set the `ZOOBINDIR` environment variable and execute the env script before compiling

## The Current Application

### Task Directory

The `task` directory contains the definitions of "Jobs" classes that the client programs can construct an object and (serialize) submit to the distributed platform through the ZooKeeper. For the purpose of this project, we have a Monte Carlo based computation of the value of pi defined in `MCPi.java`.

### Client Directory

The `clnt` directory contains the client java program (`DistClient.java`) that creates the "job" object and submits it to the platform, awaits the result and retrieves it. It does this by creating a sequential znode `/distXX/tasks/task-` that contains the "job" object (serialized) as the data and then waiting for a result znode to be created under it.

**Usage:**
```bash
$ ./runclnt.sh 500000
```

Where `500000` is the number of samples (points) that the Monte Carlo simulation should use (the larger the number, the more computation it needs to perform, but the more accurate the computed value of pi would be).

**Note:** The java files in `task` or `clnt` directories typically don't require modification, except for debugging purposes.

### Distributed Process Directory

The `dist` directory contains `DistProcess.java` that defines the program code that all the members in the distributed platform have. **This is where the main implementation logic resides.**

**Starting a process:**
```bash
$ ./runsrvr.sh
```

The system is designed to run processes from different machines to create a distributed platform with multiple members.

**Current Implementation:**
- If you start a `DistProcess`, it tries to become the master by creating an ephemeral znode `/distXX/master`
- A second process will detect that there is already a master, but does nothing else as of now
- The master process listens for any child znodes under `/distXX/tasks`. It picks up these znodes, unserializes the data to build the "job" object and performs the "computation". It then serializes the job object and writes it (creates) back to result znode that is the child of the task znode
- The master currently performs computation inside its callback function, which is not ideal as it blocks the ZK client library
- In the current implementation, the master/worker terminates after waiting for a few seconds. **In the production version, processes should remain running indefinitely** (Use the Unix kill command to terminate the JVM gracefully; avoid `kill -9` unless necessary)
- Make design choices that will reduce the amount of "cleanup" in the ZooKeeper to a minimal

## Implementation Requirements

### Required Changes

1. **Worker Registration:**
   - Any additional `DistProcess` started after a master exists should register as a "worker"
   - The implementation extends the current znode hierarchy (the ZK model) to support worker registration

2. **Worker Tracking:**
   - The master tracks all workers in the platform and detects when new workers join

3. **Task Assignment:**
   - The master assigns jobs to idle workers instead of executing computations itself

4. **Worker Task Execution:**
   - A worker executes only one job at a given point in time
   - After completing computation, the worker returns to idle status
   - Worker idle/busy status is tracked using ZooKeeper, enabling the master to manage job assignment effectively

5. **Task Queue Management:**
   - When no idle workers are available but jobs are pending, the master queues tasks until a worker becomes free or a new worker joins
   - Tasks are ideally prioritized in FIFO order (first-in-first-out), though strict ordering is not required

6. **Implementation Notes:**
   - TODO labels in `DistProcess.java` indicate key areas requiring implementation
   - Additional code and functions may be needed depending on the design approach

### Design Constraints and Requirements

#### Communication Protocol
- **All communication** between the member processes of the distributed computing platform, including the clients of the platform will be through the ZooKeeper ensemble (i.e., by creating/removing znodes, updating the data component of the znode, etc.)
- There should **not be any other form of communication** between them such as RMI, network sockets, files, etc.

#### ZNode Model Design
- Additional znodes may be created beyond the basic structure to support the required functionality
- The ZooKeeper model should be **simple** and focused on the core requirements
- Design emphasizes **minimizing**:
  - Messaging overhead between ZooKeeper and clients
  - Number of znodes required in the model
- The architecture should avoid unnecessary elements (such as failure handling features) that are not part of the core requirements
- Simplicity is key - an overly complex model often indicates improper use of watches and callbacks

#### Event-Driven Architecture
- The system architecture **must not contain any ongoing "polling" mechanism**
- Continuously checking for new tasks, idle workers, etc., is prohibited
- The implementation relies on **callbacks and watchers** to efficiently handle communication and receive notifications when events of interest occur

#### Logging
- The implementation includes comprehensive logging (print statements) throughout important steps to observe process behavior
- Log messages clearly demonstrate master and worker activities
- Example logging patterns are included in the codebase

## Documentation

A project report should describe the ZooKeeper model design. The report should include:

- A **diagram of the ZNode model** (tree structure)
- **Explanation** of what each znode is for (including the znode types)
- **Workflow descriptions** - who does what, when, and where
- **Team member contributions** (if applicable)

## Testing and Demonstration

The system can be demonstrated by running it across multiple machines to showcase the distributed architecture.

**Testing Setup:**
- Use **different machines** for:
  - The ZooKeeper ensemble
  - Client and DistProcess servers
  - (Multiple clients can run from the same machine)
  
**Recommended Test Scenario:**
- Run **at least 3 workers**
- Submit **4 or more client jobs simultaneously**
- Observe the task distribution and execution across workers

## System Assumptions

- Worker-to-master failover is not implemented (if the master shuts down, the system expects manual intervention)
- Platform shutdown occurs only when there are no pending client jobs
- Workers shut down only when the entire platform is shut down
- New workers can join the platform at any time (including after client jobs have been submitted), and the system must utilize these workers

## Useful Documentation/References

1. **ZooKeeper TextBook (O'Reilly)** Chapters 2, 3 - discuss master-worker implementations (this project uses a simpler approach)

2. **ZooKeeper Java API doc:** https://zookeeper.apache.org/doc/r3.6.2/

3. **ZooKeeper main APIs (create, get, etc.):** https://zookeeper.apache.org/doc/r3.6.2/apidocs/zookeeper-server/org/apache/zookeeper/ZooKeeper.html

4. **Link to All ZK packages and classes:** https://zookeeper.apache.org/doc/r3.6.2/apidocs/zookeeper-server/index.html

5. **AsyncCallback interfaces (to receive asynchronous callback):** https://zookeeper.apache.org/doc/r3.6.2/apidocs/zookeeper-server/org/apache/zookeeper/AsyncCallback.html

6. **Watcher Related:**
   - https://zookeeper.apache.org/doc/r3.6.2/apidocs/zookeeper-server/org/apache/zookeeper/Watcher.html
   - https://zookeeper.apache.org/doc/r3.6.2/apidocs/zookeeper-server/org/apache/zookeeper/WatchedEvent.html
