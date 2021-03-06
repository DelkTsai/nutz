package org.nutz.http;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.util.Map;

import org.nutz.lang.Encoding;
import org.nutz.lang.Lang;
import org.nutz.lang.Streams;
import org.nutz.lang.Strings;

public class Response {
    private static final String DEF_PROTOCAL_VERSION = "HTTP/1.1";

    public Response(HttpURLConnection conn, Map<String, String> reHeader) throws IOException {
        status = conn.getResponseCode();
        detail = conn.getResponseMessage();
        this.header = Header.create(reHeader);
        String s = header.get("Set-Cookie");
        if (null != s)
            this.cookie = new Cookie(s);
    }

    private Header header;
    private InputStream stream;
    private Cookie cookie;
    private String protocal = DEF_PROTOCAL_VERSION;
    private int status;
    private String detail;
    private String content;

    public String getProtocal() {
        return protocal;
    }

    public int getStatus() {
        return status;
    }

    public String getDetail() {
        return detail;
    }

    public boolean isOK() {
        return status == 200;
    }

    public boolean isServerError() {
        return status >= 500 && status < 600;
    }

    public boolean isClientError() {
        return status >= 400 && status < 500;
    }

    void setStream(InputStream stream) {
        this.stream = stream;
    }

    public Header getHeader() {
        return header;
    }

    /**
     * 根据Http头的Content-Type获取网页的编码类型，如果没有设的话则返回null
     */
    public String getEncodeType() {
        String contextType = header.get("Content-Type");
        if (null != contextType) {
        	for (String tmp : contextType.split(";")) {
        		if (tmp == null)
        			continue;
				tmp = tmp.trim();
				if (tmp.startsWith("charset="))
					return Strings.trim(tmp.substring(8)).trim();
			}
        }
        return null;
    }

    public InputStream getStream() {
        return new BufferedInputStream(stream);
    }

    public Reader getReader() {
        String encoding = this.getEncodeType();
        if (null == encoding)
            return getReader(Encoding.defaultEncoding());
        else
            return getReader(encoding);
    }

    public Reader getReader(String charsetName) {
        return new InputStreamReader(getStream(), Charset.forName(charsetName));
    }

    public Cookie getCookie() {
        return cookie;
    }

    public void printHeader(Writer writer) {
        try {
            writer.write(header.toString());
        }
        catch (IOException e) {
            throw Lang.wrapThrow(e);
        }
    }

    public void print(Writer writer) {
        print(writer, null);
    }

    public void print(Writer writer, String charsetName) {
        Reader reader = null;
        try {
            if (null == charsetName)
                reader = getReader();
            else
                reader = this.getReader(charsetName);
            int c;
            char[] buf = new char[8192];
            while (-1 != (c = reader.read(buf))) {
                writer.write(buf, 0, c);
            }
            writer.flush();
        }
        catch (IOException e) {
            throw Lang.wrapThrow(e);
        }
    }

    public String getContent() {
        if (Strings.isBlank(content)) {
            content = getContent(null);
        }
        return content;
    }

    public String getContent(String charsetName) {
        if (charsetName == null)
            return Streams.readAndClose(getReader());
        return Streams.readAndClose(getReader(charsetName));
    }
}
