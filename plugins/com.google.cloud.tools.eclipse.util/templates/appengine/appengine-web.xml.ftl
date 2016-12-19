<?xml version="1.0" encoding="utf-8"?>
<appengine-web-app xmlns="http://appengine.google.com/ns/1.0">

  <threadsafe>true</threadsafe>
  <sessions-enabled>false</sessions-enabled>
<#if service??>  <service>${service}</service>
</#if>

</appengine-web-app>