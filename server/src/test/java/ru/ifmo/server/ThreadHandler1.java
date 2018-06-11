package ru.ifmo.server;

public class ThreadHandler1 implements Handler{

        @Override
        public void handle(Request request, Response response){
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                System.out.println("Thread is Interrupted during sleep");
            }
            MultithreadingTest.isFinishedThread1 = true;
            synchronized (MultithreadingTest.monitor){
                MultithreadingTest.monitor.notify();
            }
        }
    }
