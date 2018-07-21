/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tcpserver;

/**
 *
 * @author dongluu
 */
public class ConnectList {
    protected String _ip;
    protected String _port;
    protected String _fileName;
    
    public ConnectList()
    {
        this._ip = "";
        this._port = "";
        this._fileName = "";
    }

    public ConnectList(String _ip, String _port, String _fileName) {
        this._ip = _ip;
        this._port = _port;
        this._fileName = _fileName;
    }

    public void setIp(String _ip) {
        this._ip = _ip;
    }

    public void setPort(String _port) {
        this._port = _port;
    }

    public void setFileName(String _fileName) {
        this._fileName = _fileName;
    }

    public String getIp() {
        return _ip;
    }

    public String getPort() {
        return _port;
    }

    public String getFileName() {
        return _fileName;
    }
}
