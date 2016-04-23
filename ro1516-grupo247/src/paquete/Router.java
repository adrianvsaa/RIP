package paquete;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.Set;

public class Router {
	private InetAddress direccionLocal;
	private LinkedHashMap<String, FilaTabla> tablaEncaminamiento;
	private File ficheroConf;
	private int puerto;
	private LinkedHashMap<InetAddress,Integer> vecinos;
	private DatagramSocket socket;
	
	public Router(){
		this(5512);
	}
	
	public Router(int puerto){
		try {
			NetworkInterface eth = NetworkInterface.getByName("eth0");
			Enumeration<InetAddress> direcciones = eth.getInetAddresses();
			direccionLocal = direcciones.nextElement();
			while(direcciones.hasMoreElements()){
				if(direccionLocal instanceof Inet4Address & !direccionLocal.isLoopbackAddress()){
					break;
				}
				direccionLocal = direcciones.nextElement();
			}
			ficheroConf = new File("ripconf-"+direccionLocal+".topo");
		} catch (Exception e) {
			System.err.println("Error en la captura de la direccion IP");
			//Este bloque try-catch es para que funcione en windows ya que no hay una interfaz de red que se llame eth0
			try{
				direccionLocal = InetAddress.getLocalHost();	
				ficheroConf = new File("ripconf-"+direccionLocal+".topo");
			} catch(Exception eb){
				System.err.println("Error");
			}
		}
		tablaEncaminamiento = new LinkedHashMap<String, FilaTabla>();
		vecinos = new LinkedHashMap<InetAddress, Integer>();
		try {
			socket = new DatagramSocket(puerto);
		} catch (SocketException e) {
			System.err.println("Error en socket");
		}
		this.puerto = puerto;
		leerFichero();
		imprimirTabla();
	}
	
	private void leerFichero(){
		try {
			Scanner entrada = new Scanner(ficheroConf);
			while(entrada.hasNextLine()){
				String aux = entrada.nextLine();
				if(aux.trim().split("/").length>1){
					tablaEncaminamiento.put(aux.trim().split("/")[0], new FilaTabla(aux.trim().split("/")[0], 0, "-\t", aux.trim().split("/")[1]));
				}
				else if(aux.trim().split(":").length>1){
					try{
						vecinos.put(InetAddress.getByName(aux.trim().split(":")[0]), Integer.parseInt(aux.trim().split(":")[1]));
					}catch(UnknownHostException e){
						System.err.println("Error leer fichero direccion vecinos");
					}
				}
				else{
					try{
						vecinos.put(InetAddress.getByName(aux.trim().split(":")[0]), 5512);
					}catch(UnknownHostException e){
						System.err.println("Error leer fichero direccion vecinos");
					}
				}
			}
			entrada.close();
		} catch (FileNotFoundException e) {
			System.err.println("Error en fichero "+ficheroConf.getName());
			System.exit(0);
		}
	}

	/**
	 * Este metodo inicializa dos hilos de ejecuci�n un que envie los datos de la tabla a los routers adyacentes y otro que recibira
	 * las tablas de otros routers y los procesa
	 */
	
	public void start(){
		byte[] datosRecibidos = new byte[25*5*4+4];
		DatagramPacket paqueteRecibido = new DatagramPacket(datosRecibidos, datosRecibidos.length);
		Calendar a = Calendar.getInstance();
		while(true){
			try {
				this.socket.setSoTimeout(1000*10); //Metodo que pone un limite de espera a la escucha del socket
				socket.receive(paqueteRecibido);
				Calendar b = Calendar.getInstance();
				if(b.getTimeInMillis()*1000-a.getTimeInMillis()*1000>10||actualizarTabla(paqueteRecibido.getData())){
					throw new SocketTimeoutException();
				}
			} catch (SocketTimeoutException e){
				Set<InetAddress> keys =  vecinos.keySet();
				for(InetAddress key : keys){
					DatagramPacket paqueteEnvio = new DatagramPacket(getPaquete(), getPaquete().length, key, vecinos.get(key));
					try {
						socket.send(paqueteEnvio);
					} catch (IOException e1) {
						System.err.println("Error en envio de datos");
					}
				}
			} catch (IOException e) {
				System.err.println("Error en envio o escucha datos");
			}
			a = Calendar.getInstance();
		}
	}
	
	public boolean actualizarTabla(byte[] datos){
		return true;
	}
	
	/**
	 * La funci�n retorna el paquete a enviar con la tabla de encaminamiento
	 * En este paquete no esta implementado la cryptografia ni se considera que el paquete pueda contener mas de 25 entradas el limite del paquete RIP
	 * @return
	 */
	
	public byte[] getPaquete(){
		byte[] cabecera = new byte[3];	//Los bytes 2 y 3 son bytes sin uso
		cabecera[0] = (byte) 0;			//El 1� byte va a ser un 0 porque es una respuesta
		cabecera[1] = (byte) 2;			//El 2� byte va a ser un 2 por la versi�n
		//byte[] cabecera = {(byte) 0, (byte) 2, (byte) 0, (byte) 0}; Similar a lo de arriba pero en un paso
		if(tablaEncaminamiento.size()==0)
			return null;
		byte[] entradas = new byte[tablaEncaminamiento.size()*4*5];
		Set<String> ips = tablaEncaminamiento.keySet();
		int i=0;
		for(String key: ips){
			//Addres Family y route TAG
			byte[] AFRT = {(byte) 0, (byte) 2, (byte) 0, (byte) 0};
			System.arraycopy(AFRT, 0, entradas, i, AFRT.length);
			i += 4;
			//Ip destino
			try {
				byte[] dirDestino = InetAddress.getByName(key).getAddress();
				System.arraycopy(dirDestino, 0, entradas, i, dirDestino.length);
				i += 4;
			} catch (UnknownHostException e) {
				System.err.println("Error en bytes direccion destino");
			}
			//Mascara
			byte[] masc = new byte[4];
			int x,d;
			for(x=0; x<tablaEncaminamiento.get(key).getMascara()/8; x++){
				masc[x] = (byte) 255;
			}
			if(tablaEncaminamiento.get(key).getMascara()/8<4){
				d = (int) (Math.pow(2, tablaEncaminamiento.get(key).getMascara()-x*8)-1);
				masc[x] = (byte)d;
				d = 1;
				while(x<4){
					masc[x] = (byte) 0;
					x++;
				}
			}
			System.arraycopy(masc, 0, entradas, i, masc.length);
			i += 4;

			//IP siguiente salto, esta debera ser 0.0.0.0 se debera coger del paquete la direccion origen;
			byte[] dirNextHop = {(byte) 0, (byte) 0, (byte) 0, (byte) 0};
			System.arraycopy(dirNextHop, 0, entradas, i, dirNextHop.length);
			i += 4;
			
			//Falta metrica en bytes
			byte[] metric = {(byte) 0, (byte) 0, (byte) 0, (byte) tablaEncaminamiento.get(key).getNumeroSaltos()};
			System.arraycopy(metric, 0, entradas, i, metric.length);
			i += 4;
			
		}
		byte[] paquete = new byte[cabecera.length+entradas.length];
		System.arraycopy(cabecera, 0, paquete, 0, cabecera.length);
		System.arraycopy(entradas, 0, paquete, cabecera.length, entradas.length);
		return null;
	}
	
	public int getPuerto(){
		return this.puerto;
	}
	
	public LinkedHashMap<String, FilaTabla> getTabla(){
		return this.tablaEncaminamiento;
	}

	public void imprimirTabla(){
		System.out.println("Direccion Destino\tSaltos\tNext Hop");
		Set<String> keys = tablaEncaminamiento.keySet();
		for(String key:keys){
			System.out.println(tablaEncaminamiento.get(key));
		}
	}
	
}
