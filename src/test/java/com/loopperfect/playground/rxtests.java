package com.loopperfect.playground;

import com.google.common.util.concurrent.SettableFuture;
import io.reactivex.*;
import io.reactivex.annotations.NonNull;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.Schedulers;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.math.IntMath.mod;


/**
 * Created by gaetano on 15/06/17.
 */
public class rxtests {

    class Counter {
        int c = 0;
        public Counter(){}
        public void inc(){ c++; }
        public int get(){ return c; }
    }


    @Test
    public void subscriptions() throws ExecutionException, InterruptedException {

        final Counter c = new Counter();

        final SettableFuture<Boolean> f = SettableFuture.create() ;
        SettableFuture<Integer> result = SettableFuture.create();

        ExecutorService s = Executors.newCachedThreadPool();
        Scheduler ss = Schedulers.from(s);
        final Observable<Integer> o = Observable
            .just(1,2,3,4)
            .subscribeOn(ss)
            .doOnSubscribe( x -> {
                c.inc();
            }).doOnComplete(()->{
                f.set(true);
            }).publish()
            .autoConnect();

        final Single<Integer> a = o.filter(x -> mod(x,2)==0)
            .reduce(0, (x,y) -> x+y)
            .subscribeOn(ss);
        final Single<Integer> b = o.filter(x -> mod(x,2)==1)
            .reduce(0, (x,y) -> x+y)
            .subscribeOn(ss);

        Single
            .zip(a,b, (x, y) -> x + y)
            .subscribe(r->result.set(r));

        Assert.assertTrue(f.get());
        Assert.assertEquals(10, (int)result.get());


        s.shutdown();
    }


    @Test
    public void subscriptions2() throws ExecutionException, InterruptedException {

        final Counter c = new Counter();

        ExecutorService s = Executors.newCachedThreadPool();
        Scheduler ss = Schedulers.from(s);

        Observable<Integer> x = Observable
            .just(1,2,3,4)
            .doOnNext(y-> System.out.print(y) )
            .doOnSubscribe( y -> c.inc())
            .cache();

        Observable<Integer> y = Observable.concat(
            x.skip(1),
            x.skipLast(1)
        );


        //final SettableFuture<Integer> result = SettableFuture.create();
        y.subscribe(
          //  n->{},
          //  e->{},
          //  ()->{ result.set(c.get()); }
        );


        //Thread.yield();
        //result.wait();

        Assert.assertEquals(1, (int)c.get());


    }

}
