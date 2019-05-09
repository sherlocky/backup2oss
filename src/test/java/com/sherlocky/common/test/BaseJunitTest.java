package com.sherlocky.common.test;

import com.alibaba.fastjson.JSON;
import org.junit.After;
import org.junit.Before;

/**
 * @author zhangcx
 * @date 2017-05-18
 */
public class BaseJunitTest {

    private long start = 0;
    private long end = 0;

    @Before
    public void before() {
        start = System.currentTimeMillis();
    }

    @After
    public void end() {
        end = System.currentTimeMillis();
        System.out.println("方法执行完毕，总耗时：" + (end - start) + "ms");
    }

    public void beautiful(Object obj) {
        // 默认输出 prettyFormat 格式
        this.beautiful(obj, true);
    }

    public void beautiful(Object obj, boolean prettyFormat) {
        if (obj == null) {
            println("null");
            return;
        }
        if (obj instanceof String) {
            if (!prettyFormat) {
                println(obj);
                return;
            }
            println(JSON.toJSONString(JSON.parseObject((String) obj), true));
            return;
        }
        println(JSON.toJSONString(obj, prettyFormat));
    }

    public void println(Object obj) {
        System.out.println(obj);
    }
}
