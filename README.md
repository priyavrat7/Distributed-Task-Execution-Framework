# Distributed Task Execution Framework

A distributed task execution system implemented in Java using Apache ZooKeeper for master-worker coordination, featuring event-driven task queue management, fault-tolerant worker load balancing, and multi-threaded middleware support for TCP/IP and RMI protocols.

## 📋 Overview

This framework implements a distributed computing system where tasks are submitted by clients, coordinated by a master node, and executed by worker nodes. The system uses Apache ZooKeeper for distributed coordination, ensuring fault tolerance, service discovery, and efficient task distribution across multiple worker nodes.

## ✨ Features

- **Master-Worker Architecture**: Automatic master election using ZooKeeper ephemeral znodes
- **Event-Driven Task Management**: Real-time task assignment using ZooKeeper watchers
- **Fault Tolerance**: Automatic worker failure detection and task reassignment
- **Load Balancing**: Intelligent task distribution across idle workers
- **Multi-Protocol Support**: Middleware server supporting both TCP/IP and RMI protocols
- **Service Discovery**: Dynamic worker registration and discovery using ephemeral znodes
- **Automated Deployment**: Shell scripts for ZooKeeper cluster setup and management

## 🏗️ Architecture

### System Components

```
┌──────────┐         ┌──────────────┐         ┌──────────┐
│  Client  │────────►│    Master    │────────►│  Worker  │
│          │         │  (ZooKeeper) │         │          │
└──────────┘         └──────────────┘         └──────────┘
                            │
                            ▼
                    ┌──────────────┐
                    │   ZooKeeper   │
                    │    Cluster    │
                    └──────────────┘
```

### ZNode Structure

The system uses the following ZooKeeper znode hierarchy:

```
/dist27/
├── /master              # Master election node
├── /tasks/              # Task submission queue
│   ├── task-0000000001 # Sequential task nodes
│   ├── task-0000000002
│   └── ...
├── /workers/            # Worker registration
│   ├── worker-id-1      # Ephemeral worker nodes
│   ├── worker-id-2
│   └── ...
└── /idle/               # Idle worker tracking
    ├── worker-id-1      # Ephemeral idle nodes
    └── ...
```

### Key Components

1. **Master Node**: 
   - First process to successfully create `/master` znode
   - Monitors `/tasks` for new task submissions
   - Tracks idle workers via `/idle` znodes
   - Assigns tasks to available workers

2. **Worker Nodes**:
   - Register themselves under `/workers` with unique IDs
   - Create ephemeral `/idle` nodes when available
   - Watch for task assignments
   - Execute tasks and write results to `/tasks/{task-id}/result`

3. **Client**:
   - Submits tasks by creating sequential nodes under `/tasks`
   - Retrieves results from `/tasks/{task-id}/result`

## 🛠️ Technology Stack

- **Language**: Java (80.3%)
- **Coordination**: Apache ZooKeeper 3.6.2
- **Protocols**: TCP/IP, RMI (Java Remote Method Invocation)
- **Build**: Shell scripts for compilation and deployment
- **Infrastructure**: Multi-server ZooKeeper cluster

## 🚀 Getting Started

### Prerequisites

- Java Development Kit (JDK) 8 or higher
- Apache ZooKeeper 3.6.2 or compatible version
- Multiple servers for ZooKeeper cluster (minimum 3 for production)
- Network connectivity between all nodes

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/priyavrat7/Distributed-Task-Execution-Framework.git
   cd Distributed-Task-Execution-Framework
   ```

2. **Set up ZooKeeper cluster**
   
   Update `available_servers.txt` with your server addresses:
   ```
   server.1=hostname1:22299:22399
   server.2=hostname2:22299:22399
   server.3=hostname3:22299:22399
   ```

3. **Configure ZooKeeper**
   
   Copy and customize the ZooKeeper configuration:
   ```bash
   cp zoo-base.cfg zoo.cfg
   # Edit zoo.cfg with your server addresses
   ```

4. **Initialize ZooKeeper data directories**
   ```bash
   ./server-setup.sh
   ```

### Running the System

1. **Start ZooKeeper cluster**
   ```bash
   ./server-start.sh
   ```

2. **Compile the Java code**
   ```bash
   ./compile.sh
   ```

3. **Start Master/Worker nodes**
   ```bash
   # On each node, run the Java application
   # The first node to start becomes the master
   java -cp <classpath> MainClass
   ```

4. **Submit tasks via client**
   ```bash
   ./client-create-nodes.sh
   ```

5. **Stop the system**
   ```bash
   ./server-stop.sh
   ```

## 📖 Usage

### Master-Worker Coordination

The system automatically handles master election:
- Each node attempts to create the `/master` znode
- The first successful node becomes the master
- Other nodes become workers
- If master fails, workers can re-elect a new master

### Task Submission

Tasks are submitted by creating sequential znodes under `/tasks`:
- Each task contains serialized task data
- Master monitors new tasks via watchers
- Tasks are assigned to idle workers automatically

### Worker Management

- Workers register with unique IDs (format: `PORT@LAB`)
- Idle workers create ephemeral nodes under `/idle`
- Master assigns tasks by updating worker znode data
- Worker deletion of idle node triggers task execution
- Results are written to `/tasks/{task-id}/result`

## 🔧 Configuration

### ZooKeeper Configuration

Edit `zoo-base.cfg` or create `zoo.cfg`:

```properties
tickTime=2000
dataDir=/path/to/zookeeper/data
clientPort=2181
initLimit=5
syncLimit=2
server.1=hostname1:22299:22399
server.2=hostname2:22299:22399
server.3=hostname3:22299:22399
```

### Server Configuration

Update `available_servers.txt` with your cluster servers:
```
server.1-lab2-25.cs.mcgill.ca:22299:22399
server.2-lab2-26.cs.mcgill.ca:22299:22399
server.3-lab2-29.cs.mcgill.ca:22299:22399
```

## 📁 Project Structure

```
Distributed-Task-Execution-Framework/
├── zk/                          # ZooKeeper related files
├── compile.sh                   # Compilation script
├── server-setup.sh              # ZooKeeper cluster setup
├── server-start.sh              # Start ZooKeeper servers
├── server-stop.sh               # Stop ZooKeeper servers
├── server-availability.sh       # Check server availability
├── client-create-nodes.sh       # Client task submission script
├── available_servers.txt         # Server configuration
├── zoo-base.cfg                 # Base ZooKeeper configuration
├── zoo-base-source.cfg          # Source ZooKeeper configuration
├── README.md                    # This file
└── [Java source files]          # Main implementation
```

## 🔐 Fault Tolerance

- **Master Failure**: Automatic re-election when master node fails
- **Worker Failure**: Ephemeral znodes automatically removed on worker failure
- **Task Recovery**: Master can reassign tasks from failed workers
- **Network Partitions**: ZooKeeper handles split-brain scenarios

## 🧪 Testing

1. **Start ZooKeeper cluster** on multiple servers
2. **Launch multiple worker nodes** across different machines
3. **Submit tasks** using the client script
4. **Monitor task execution** via ZooKeeper CLI or Java API
5. **Test fault tolerance** by killing master or worker nodes

## 📝 Key Algorithms

### Master Task Assignment

```java
// Pseudo-code
onTaskCreated() {
    taskQueue.add(newTask);
    assignTasksToIdleWorkers();
}

onIdleWorkerAvailable() {
    idleWorkerCache.update();
    assignTasksToIdleWorkers();
}

assignTasksToIdleWorkers() {
    while (!taskQueue.isEmpty() && !idleWorkerCache.isEmpty()) {
        task = taskQueue.poll();
        worker = idleWorkerCache.get();
        assignTask(worker, task);
        deleteIdleNode(worker);
    }
}
```

### Worker Task Execution

```java
// Pseudo-code
onIdleNodeDeleted() {
    taskId = getTaskIdFromWorkerNode();
    task = getTaskData(taskId);
    result = executeTask(task);
    writeResult(taskId, result);
    recreateIdleNode();
}
```

## 🤝 Contributing

This project was developed as part of COMP512 (Distributed Systems) coursework at McGill University.

Contributors:
- Priyavrat Dev Sharma

## 📄 License

This project is part of academic coursework. Please refer to the license file for details.

## 🙏 Acknowledgments

- McGill University, Department of Computer Science
- Apache ZooKeeper project for excellent distributed coordination framework

## 📚 Related Documentation

- [ZooKeeper Setup Guide](./ZookeeperSetup-Distributed.pdf)
- [Project Report](./p3.pdf)
- [Apache ZooKeeper Documentation](https://zookeeper.apache.org/doc/current/)

## 🔗 Links

- **Repository**: [https://github.com/priyavrat7/Distributed-Task-Execution-Framework](https://github.com/priyavrat7/Distributed-Task-Execution-Framework)
- **Issues**: [Report a bug or request a feature](https://github.com/priyavrat7/Distributed-Task-Execution-Framework/issues)

---

**Note**: This framework is designed for educational and research purposes. For production use, ensure proper security configurations and error handling.

