
<%@ page language="java" import="java.util.*" %>
<%
   Date today= new Date();
   String time = "Now is: "+ today.getHours()+":"+today.getMinutes()+":"+today.getSeconds();
   out.println(time);
%>