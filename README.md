### fgb is a rpc framework base on rabbitmq and spring boot amqp 
> fgb mains front ground bridge, using this you can rpc anything with out limited auth things like **oauth2**

#### @FgbClient
```java
package com.gaad.fgb.demo.demo.fgb;

import com.gaad.rabbitmq.fgb.annotation.FgbClient;

import com.gaad.rabbitmq.fgb.annotation.FgbClientMethod;
import net.unsun.infrastructure.common.kit.ResultBean;

import java.util.List;

/**
 * IndexFgbClient
 */
@FgbClient("demo.indexFgb")
public interface IndexFgbClient {

    @FgbClientMethod
    ResultBean<List<String>> testIndex();
}

``` 
#### @FgbServer
```java
package com.gaad.fgb.demo.demo.fgb;

import com.gaad.rabbitmq.fgb.annotation.FgbServer;

import com.gaad.rabbitmq.fgb.annotation.FgbServerMethod;
import net.unsun.infrastructure.common.kit.ResultBean;

import java.util.ArrayList;
import java.util.List;

/**
 * IndexFgbServer
 */
@FgbServer("demo.indexFgb")
public class IndexFgbServer {

    @FgbServerMethod
    public ResultBean<List<String>> testIndex() {
        ResultBean<List<String>> result = new ResultBean<>();
        List<String> list  = new ArrayList<>();
        list.add("i am indexFgbServer");
        result.setData(list);
        return result;
    }
    
}

```
####  Controller( in controller you can @Autowired the interface of @FgbClient )
```java
package com.gaad.fgb.demo.demo.controller;

import com.gaad.fgb.demo.demo.fgb.IndexFgbClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.unsun.infrastructure.common.kit.ResultBean;

import java.util.List;

/**
 * IndexController
 */
@RestController
@RequestMapping("/")
public class IndexController {

    @Autowired
    IndexFgbClient indexFgbClient;

    @GetMapping("/index")
    public ResultBean<List<String>> index() {
        return indexFgbClient.testIndex();
    }
    
}
```
> top is the simple demo for fgb
