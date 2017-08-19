# Create a Job
A unit of work is encapsulated by a JobInfo object. This object specifies the scheduling criteria. The job scheduler allows to consider the state of the device, e.g., if it is idle or if network is available at the moment. Use the JobInfo.Builder class to configure how the scheduled task should run. You can schedule the task to run under specific conditions, such as:
* Device is charging
* Device is connected to an unmetered network
* Device is idle
* Start before a certain deadline
* Start within a predefined time window, e.g., within the next hour
* Start after a minimal delay, e.g., wait a minimum of 10 minutes

To implement a `Job`, extend the `JobService` class and implement the `onStartJob` and `onStopJob`. If the job fails for some reason, return true from on the `onStopJob` to restart the job. The `onStartJob` is performed in the main thread, if you start asynchronous processing in this method, return true otherwise false.

The new JobService must be registered in the Android manifest with the BIND_JOB_SERVICE permission.
```xml
<service 
        android:name=".TestJobService"
        android:label="Word service"
        android:permission="android.permission.BIND_JOB_SERVICE" >
</service>
```