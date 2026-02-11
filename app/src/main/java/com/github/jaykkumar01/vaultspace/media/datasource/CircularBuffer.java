package com.github.jaykkumar01.vaultspace.media.datasource;

public final class CircularBuffer {

    private final byte[] buffer;
    private final Object lock=new Object();

    private int writePos,readPos,available;
    private int prefetchLimit;
    private boolean eof;

    public CircularBuffer(int capacity){
        this.buffer=new byte[capacity];
        this.prefetchLimit=capacity;
    }

    public void setPrefetchLimit(int bytes){
        synchronized(lock){ this.prefetchLimit=bytes; }
    }

    public void write(byte[] src,int length)throws InterruptedException{
        synchronized(lock){
            while(available+length>prefetchLimit) lock.wait();

            int first=Math.min(length,buffer.length-writePos);
            System.arraycopy(src,0,buffer,writePos,first);

            int remain=length-first;
            if(remain>0)
                System.arraycopy(src,first,buffer,0,remain);

            writePos=(writePos+length)%buffer.length;
            available+=length;

            lock.notifyAll();
        }
    }

    public int read(byte[] target,int offset,int length)throws InterruptedException{
        synchronized(lock){
            while(available==0&&!eof) lock.wait();
            if(available==0) return -1;

            int toRead=Math.min(length,available);

            int first=Math.min(toRead,buffer.length-readPos);
            System.arraycopy(buffer,readPos,target,offset,first);

            int remain=toRead-first;
            if(remain>0)
                System.arraycopy(buffer,0,target,offset+first,remain);

            readPos=(readPos+toRead)%buffer.length;
            available-=toRead;

            lock.notifyAll();
            return toRead;
        }
    }

    public void reset(){
        synchronized(lock){
            writePos=readPos=available=0;
            eof=false;
            lock.notifyAll();
        }
    }

    public void signalEof(){
        synchronized(lock){
            eof=true;
            lock.notifyAll();
        }
    }

    public boolean isEof(){ return eof; }

    public int getAvailable(){ return available; }
}
