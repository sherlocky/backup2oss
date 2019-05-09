## my.cnf 文件中需要指定客户端连接信息。。
/usr/bin/mysqldump --defaults-extra-file=/etc/my.cnf ghost-blog-new | gzip > /opt/backup/mysql/db_ghost-blog-new-$(date +%Y%m%d_%H%M%S).sql.gz
echo 'complete'