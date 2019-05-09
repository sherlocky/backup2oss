# backup2oss
[![experimental](http://badges.github.io/stability-badges/dist/experimental.svg)](http://github.com/badges/stability-badges)
[![MIT Licence](https://badges.frapsoft.com/os/mit/mit.svg?v=103)](https://opensource.org/licenses/mit-license.php)

#### 介绍
使用 Springboot2 结合 qiniu 上传 API 定时备份 ghost 博客数据库到七牛OSS。

#### TODO 
- 定期清理过期备份，每月只保留一个备份。。

#### 注意事项
需要结合数据库备份脚本公共实现（本程序只负责加密、上传数据库备份文件到七牛，不实现数据库备份功能）
> 可参考 ``src/main/resources/linux/`` 下脚本