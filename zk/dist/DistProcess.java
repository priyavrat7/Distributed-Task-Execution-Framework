
/*
Copyright
All materials provided to the students as part of this course is the property of respective authors. Publishing them to third-party (including websites) is prohibited. Students may save it for their personal use, indefinitely, including personal cloud storage spaces. Further, no assessments published as part of this course may be shared with anyone else. Violators of this copyright infringement may face legal actions in addition to the University disciplinary proceedings.
©2022, Joseph D’Silva
*/
import java.io.*;

import java.util.*;
import java.util.HashMap;

// To get the name of the host.
import java.net.*;

//To get the process id.
import java.lang.management.*;

import org.apache.zookeeper.*;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.KeeperException.*;
import org.apache.zookeeper.data.*;
import org.apache.zookeeper.KeeperException.Code;
import java.nio.charset.StandardCharsets;

// TODO
// Replace XX with your group number.
// You may have to add other interfaces such as for threading, etc., as needed.
// This class will contain the logic for both your master process as well as the worker processes.
//  Make sure that the callbacks and watch do not conflict between your master's logic and worker's logic.
//		This is important as both the master and worker may need same kind of callbacks and could result
//			with the same callback functions.
//	For a simple implementation I have written all the code in a single class (including the callbacks).
//		You are free it break it apart into multiple classes, if that is your programming style or helps
//		you manage the code more modularly.
//	REMEMBER !! ZK client library is single thread - Watches & CallBacks should not be used for time consuming tasks.
//		Ideally, Watches & CallBacks should only be used to assign the "work" to a separate thread inside your program.
public class DistProcess implements Watcher {
  String workersName;
  String workersPath;
  ZooKeeper zk;
  String zkServer, pinfo;
  boolean checkingMaster = false;

  // Hashmap (Key = workersName, Value = "Idle" or "task_node_name")
  HashMap<String, String> hashmapofWorkers = new HashMap<>();

  // Queue of sting to store pending tasks
  Queue<String> awaitedTasksQueue = new LinkedList<String>();

  // ArrayList of Initialised tasks
  ArrayList<String> assignedTasksAR = new ArrayList<String>();

  DistProcess(String zkhost) {
    zkServer = zkhost;
    pinfo = ManagementFactory.getRuntimeMXBean().getName();
    System.out.println("DISTAPP : ZK Connection information : " + zkServer);
    System.out.println("DISTAPP : Process information : " + pinfo);
    // pinfo contails pid with server name
  }

  void startProcess() throws IOException,
      UnknownHostException,
      KeeperException,
      InterruptedException {
    zk = new ZooKeeper(zkServer, 1000, this); // connection to the Zookeeper
    try {
      runForMaster(); // See if you can become the master (i.e, no other master exists)
      checkingMaster = true;
      if (checkingMaster == true)
        System.out.println("DISTAPP : Role : " + " I will be functioning as Master");
      else
        System.out.println("DISTAPP : Role : " + " I will be functioning as Worker");

      // System.out.println("DISTAPP : Role : " + " I will be functioning as "
      // +(checkingMaster?"master":"worker"));
      fetchWorkersbyMaster();
      fetchTaskbyMaster();
    } catch (NodeExistsException nodeExistExp) {
      checkingMaster = false;
      if (checkingMaster == true)
        System.out.println("DISTAPP : Role : " + " I will be functioning as Master");
      else
        System.out.println("DISTAPP : Role : " + " I will be functioning as Worker");
      // System.out.println("DISTAPP : Role : " + " I will be functioning as "
      // +(checkingMaster?"master":"worker"));
      // Initialitiong the worker node and replace the path at it is initialised
      workersPath = zk.create("/dist27/workers/worker-", "idle".getBytes(), Ids.OPEN_ACL_UNSAFE,
          CreateMode.EPHEMERAL_SEQUENTIAL);
      workersName = workersPath.replace("/dist27/workers/", "");
      // Fetching task
      workergetTaskData(workersName);
      // Print the exception log
      nodeExistExp.printStackTrace();

    }
  }

  // Try to become the master.
  void runForMaster() throws UnknownHostException,
      KeeperException,
      InterruptedException,
      NodeExistsException {
    // Try to create an ephemeral node to be the master, put the hostname and pid of
    // this process as the data.
    // This is an example of Synchronous API invocation as the function waits for
    // the execution and no callback is involved..
    zk.create("/dist27/master", pinfo.getBytes(), Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
  }

  void fetchTaskbyMaster() {
    // Getting the info of Children by calling asynchronously(using call back) with
    // a watcher
    zk.getChildren("/dist27/tasks", watcherofMaster, callbackofMaster, null);
  }

  void fetchWorkersbyMaster() {
    // Getting the info of Children by calling asynchronously(using call back) with
    // a watcher
    zk.getChildren("/dist27/workers", watcherofMastertogetWorker, callbackofMastertogetWorker, null);
  }

  void workergetTaskData(String workersName) {
    // Getting the info of Children by calling asynchronously(using call back) with
    // a watcher
    zk.getData("/dist27/workers/" + workersName, watcherofWorkerTask, callbackofWorkerTask, null);
  }

  // Setting different watchers
  Watcher watcherofMastertogetWorker = new Watcher() {
    public void process(WatchedEvent e) {
      System.out.println("DISTAPP : Event received : " + e);
      if (e.getType() == Watcher.Event.EventType.NodeChildrenChanged && e.getPath().equals("/dist27/workers")) {
        System.out.println(
            "Children of worker node has been changed, hence reinstalling the watcher and send command to get worker's children");
        fetchWorkersbyMaster();
      }
    }
  };

  Watcher watcherofMaster = new Watcher() {
    public void process(WatchedEvent e) {
      System.out.println("DISTAPP : An Event received : " + e);
      if (e.getType() == Watcher.Event.EventType.NodeChildrenChanged && e.getPath().equals("/dist27/tasks")) {
        System.out.println(
            "Children of task node has been changed, hence reinstalling the watcher and send command to get tasks or children");
        fetchTaskbyMaster();
      }
    }
  };

  Watcher taskCompletionWatcherM = new Watcher() {
    public void process(WatchedEvent e) {
      System.out.println("DISTAPP : Event received : " + e);
      if (e.getType() == Watcher.Event.EventType.NodeDataChanged) {
        String worker_name = e.getPath().replace("/dist27/workers/", "");
        zk.getData("/dist27/workers/" + worker_name, taskCompletionWatcherM, taskCompletionCallbackM, null);
      }
    }
  };

  Watcher watcherofWorkerTask = new Watcher() {
    public void process(WatchedEvent e) {
      System.out.println("DISTAPP : Event received : " + e);
      if (e.getType() == Watcher.Event.EventType.NodeDataChanged && e.getPath().equals(workersPath)) {
        workergetTaskData(workersName);
      }
    }
  };

  // Watcher method
  // This Watcher method is for the watcher setup when instantiating the ZooKeeper
  // object.
  public void process(WatchedEvent e) {
    // Get watcher notifications.
    // !! IMPORTANT !!
    // Do not perform any time consuming/waiting steps here
    // including in other functions called from here.
    // Your will be essentially holding up ZK client library
    // thread and you will not get other notifications.
    // Instead include another thread in your program logic that
    // does the time consuming "work" and notify that thread from here.
    System.out.printf("DISTAPP : Event received : " + e);
  }

  AsyncCallback.ChildrenCallback callbackofMaster = new AsyncCallback.ChildrenCallback() {
    public void processResult(int rc, String path, Object ctx, List<String> children) {
      // TODO
      System.out
          .println("DISTAPP : callbackofMaster: processResult : ChildrenCallback : " + rc + ":" + path + ":" + ctx);
      for (String nameofTask : children) {
        try {
          // Check data structure to see if it is a new task
          boolean is_oldTask = assignedTasksAR.contains(nameofTask);

          // if it is a new task
          if (!is_oldTask) {
            assignedTasksAR.add(nameofTask);
            awaitedTasksQueue.offer(nameofTask);
            boolean is_idleWorkerAvailable = hashmapofWorkers.containsValue("idle");
            // If there is an idle worker
            if (is_idleWorkerAvailable) {
              String idle_worker_name = findIdleWorker(hashmapofWorkers, "idle");
              String upcomingTask = awaitedTasksQueue.poll();
              hashmapofWorkers.replace(idle_worker_name, upcomingTask);
              zk.setData("/dist27/workers/" + idle_worker_name, upcomingTask.getBytes(), -1);
              zk.getData("/dist27/workers/" + idle_worker_name, taskCompletionWatcherM, taskCompletionCallbackM, null);
            }
          }
          // If it is an old task then nothing is needed to be done
          if (is_oldTask) {
            // Leaving it
          }
        } catch (NodeExistsException nodeExistException) {
          nodeExistException.printStackTrace();
        } catch (KeeperException keeperException) {
          keeperException.printStackTrace();
        } catch (InterruptedException interruptedException) {
          interruptedException.printStackTrace();
        }
      }
    }
  };

  AsyncCallback.DataCallback taskCompletionCallbackM = new AsyncCallback.DataCallback() {
    public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
      System.out.println("DISTAPP : taskCompletionCallbackM: processResult : DataCallback : " + rc + ":" + path + ":"
          + ctx + ":" + stat);
      try {
        // Check if the DATA of the worker node is "idle"
        String worker_status = new String(data, StandardCharsets.UTF_8);
        boolean task_completed = "idle".equals(worker_status);
        // Idle status of wroker can be counted as the task is finished
        if (task_completed) {
          String worker_name = path.replace("/dist27/workers/", "");
          hashmapofWorkers.replace(worker_name, "idle");
          if (awaitedTasksQueue.size() > 0) {
            String upcomingTask = awaitedTasksQueue.poll();
            hashmapofWorkers.replace(worker_name, upcomingTask);
            zk.setData("/dist27/workers/" + worker_name, upcomingTask.getBytes(), -1);
            zk.getData("/dist27/workers/" + worker_name, taskCompletionWatcherM, taskCompletionCallbackM, null);
          }
        } else {
          // Worker is not idle, that means the task is being executed
        }
      } catch (NodeExistsException nodeExistException) {
        nodeExistException.printStackTrace();
      } catch (KeeperException keeperException) {
        keeperException.printStackTrace();
      } catch (InterruptedException interruptedException) {
        interruptedException.printStackTrace();
      }
    }
  };

  public String findIdleWorker(Map<String, String> map, String value) {

    Set<String> result = new HashSet<>();
    for (Map.Entry<String, String> entry : map.entrySet()) {
      if (Objects.equals(entry.getValue(), value)) {
        result.add(entry.getKey());
        return entry.getKey();
      }
    }
    return null;
  }

  AsyncCallback.ChildrenCallback callbackofMastertogetWorker = new AsyncCallback.ChildrenCallback() {
    public void processResult(int rc, String path, Object ctx, List<String> children) {
      System.out.println(
          "DISTAPP : callbackofMastertogetWorker : processResult : ChildrenCallback : " + rc + ":" + path + ":" + ctx);
      for (String c : children) {
        try {
          boolean checkNewWorker = !(hashmapofWorkers.containsKey(c)); // Checking in the Hash map if there is a new
                                                                       // worker
          if (checkNewWorker) {
            // Add the worker to the HashMap
            // Set the status of the worker to "idle" in the HashMap
            hashmapofWorkers.put(c, "idle");
            System.out.println("There is a new worker with idle status and added into the hashmap of workers");

            // System.out.println("Checking the awaited task, if any");
            if (awaitedTasksQueue.size() > 0) {
              // get the topmost pending task from the queue, and dequeue it.
              System.out.println("Polling the pending task");
              String task = awaitedTasksQueue.poll();
              System.out.println("Set the status of worker with the task, just polled");
              hashmapofWorkers.replace(c, task);
              // Set the DATA of the worker node to the "task name"
              // Set the data for the node of the given path if such a node exists and the
              // given version matches the version
              // of the node (if the given version is -1, it matches any node's versions).
              zk.setData("/dist27/workers/" + c, task.getBytes(), -1);
              // Invoking getData with watcher and callback to keep track of worker, whenever
              // it finishes task
              zk.getData("/dist27/workers/" + c, taskCompletionWatcherM, taskCompletionCallbackM, null);
            }
          } else {
            System.out.println("There is no new worker, so nothing is to be done as per the guidelines provided");
          }
        } catch (NodeExistsException nodeExistException) {
          nodeExistException.printStackTrace();
        } catch (KeeperException keeperException) {
          keeperException.printStackTrace();
        } catch (InterruptedException interruptedException) {
          interruptedException.printStackTrace();
        }
      }
    }
  };

  AsyncCallback.DataCallback callbackofWorkerTask = new AsyncCallback.DataCallback() {
    public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
      System.out.println("DISTAPP : callbackofWorkerTask : processResult : DataCallback : " + rc + ":" + path + ":"
          + ctx + ":" + stat);

      // Check the Data of the worker node to see if the worker has been assigned a
      // task
      String worker_status = new String(data, StandardCharsets.UTF_8);
      boolean hastask_assigned = !("idle".equals(worker_status));
      if (hastask_assigned) {
        // Create a new thread on which the task is completed
        Thread execute_task = new Thread(() -> {
          try {
            byte[] serializationofTask = zk.getData("/dist27/tasks/" + worker_status, false, null);
            ByteArrayInputStream byteInputStreamofTask = new ByteArrayInputStream(serializationofTask);
            ObjectInput inputTask = new ObjectInputStream(byteInputStreamofTask);
            DistTask dt = (DistTask) inputTask.readObject();
            dt.compute();
            // Serialize our Task object back to a byte array!
            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteOutputStream);
            objectOutputStream.writeObject(dt);
            System.out.println("Flushing the output stream");
            objectOutputStream.flush();
            serializationofTask = byteOutputStream.toByteArray();
            // Save it inside the resultNode
            zk.create("/dist27/tasks/" + worker_status + "/result", serializationofTask, Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT);
            // Setting the status of worker node to idle, hence master node can deduce that
            // the task has been completed.
            zk.setData("/dist27/workers/" + workersName, "idle".getBytes(), -1);
          } catch (NodeExistsException nodeExistException) {
            nodeExistException.printStackTrace();
          } catch (KeeperException keeperException) {
            keeperException.printStackTrace();
          } catch (InterruptedException interruptedException) {
            interruptedException.printStackTrace();
          } catch (IOException inpout) {
            inpout.printStackTrace();

          } catch (ClassNotFoundException classNotFound) {
            classNotFound.printStackTrace();
          }
        }); // end of thread
        // Run thread
        execute_task.start();
      } else {
        // If the worker has not been assigned a task
        // do nothing, a watcher has already been set
      }
    }
  };

  public static void main(String args[]) throws Exception {
    // Create a new process
    // Read the ZooKeeper ensemble information from the environment variable.
    DistProcess dt = new DistProcess(System.getenv("ZKSERVER"));
    dt.startProcess();
    // Thread.sleep(250000);
    while (true) {
      // do not do anything
    }
  }
}