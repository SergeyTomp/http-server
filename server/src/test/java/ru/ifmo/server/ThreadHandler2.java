package ru.ifmo.server;
public class ThreadHandler2  implements Handler{

        @Override
        public void handle(Request request, Response response){
            MultithreadingTest.isFinishedThread2 = true;
            synchronized (MultithreadingTest.monitor){
                MultithreadingTest.monitor.notify();
            }
        }
    }
