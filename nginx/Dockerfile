FROM nginx:alpine

RUN apk add --no-cache logrotate

COPY logrotate-nginx.conf /etc/logrotate.d/nginx

RUN echo "0 3 * * * /usr/sbin/logrotate /etc/logrotate.d/nginx" > /etc/crontabs/root

CMD crond && nginx -g "daemon off;"
