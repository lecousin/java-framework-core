/**
 * Multi-threading.
 * 
 * <p>
 * The multi-threading system is based on:<ul>
 * <li>Resource:
 * 	a physical resource such as CPU or drive
 * </li>
 * <li>{@link net.lecousin.framework.concurrent.threads.Task}:
 * 	unit of work on a resource
 * </li>
 * <li>{@link net.lecousin.framework.concurrent.threads.priority.TaskPriorityManager}:
 *  Handle priority between tasks
 * </li>
 * <li>{@link net.lecousin.framework.concurrent.threads.TaskExecutor}:
 *  Thread executing tasks
 * </li>
 * <li>{@link net.lecousin.framework.concurrent.threads.TaskManager}:
 *  Handle tasks for a given resource, based on a TaskPriorityManager, and managing a list of TaskExecutor
 * </li>
 * <li>{@link net.lecousin.framework.concurrent.threads.TaskScheduler}:
 *  Launch tasks at specific schedules
 * </li>
 * </ul>
 * </p>
 */
package net.lecousin.framework.concurrent.threads;
