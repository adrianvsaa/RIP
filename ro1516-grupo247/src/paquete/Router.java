package paquete;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.Set;

public class Router {
	private InetAddress direccion;
	private String direccionLocal;
	private LinkedHashMap<String, FilaTabla> tablaEncaminamiento;
	private File ficheroConf;
	private int puerto;
	//private DatagramSocket socket;
	
	public Router(){
		this(5512);
	}
	
	public Router(int puerto){
		try {
			NetworkInterface eth = NetworkInterface.getByName("eth0");
			Enumeration<InetAddress> direcciones = eth.getInetAddresses();
			direccion = direcciones.nextElement();
			while(direcciones.hasMoreElements()){
				if(direccion instanceof Inet4Address & !direccion.isLoopbackAddress()){
					break;
				}
				direccion = direcciones.nextElement();
			}
			direccionLocal = direccion.toString().split("/")[1];
			ficheroConf = new File("ripconf-"+direccionLocal+".topo");
		} catch (Exception e) {
			System.err.println("Error en la captura de la direccion IP");
			//Este bloque try-catch es para que funcione en windows ya que no hay una interfaz de red que se llame eth0
			try{
				direccion = InetAddress.getLocalHost();
				direccionLocal = direccion.toString().split("/")[1];		
				ficheroConf = new File("ripconf-"+direccionLocal+".topo");
			} catch(Exception eb){
				System.err.println("Error");
			}
		}
		tablaEncaminamiento = new LinkedHashMap<String, FilaTabla>();
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
					tablaEncaminamiento.put(aux.trim().split("/")[0], new FilaTabla(aux.trim().split("/")[0], 0, "-\t", 0, aux.trim().split("/")[1]));
				}
				else if(aux.trim().split(":").length>1){
					tablaEncaminamiento.put(aux.trim().split(":")[0], new FilaTabla(aux.trim().split(":")[0], 1, aux.trim().split(":")[0],
							Integer.parseInt(aux.trim().split(":")[1]), "0"));
				}
				else{
					tablaEncaminamiento.put(aux.trim(), new FilaTabla(aux.trim(), 1, aux.trim(), 5512, "0"));
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
	
	public void startService(){
			Thread enviar = new HiloEnviar(this);
			enviar.start();
			Thread recibir = new HiloRecibir(this);
			recibir.start();
	}
	
	public void actualizarTabla(byte[] datos){
		
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
			//Ip destino
			try {
				byte[] dirDestino = InetAddress.getByName(key).getAddress();
				System.arraycopy(dirDestino, 0, entradas, i, dirDestino.length);
				i += 4;
			} catch (UnknownHostException e) {
				System.err.println("Error en bytes direccion destino");
			}
			//Mascara
			String mascara="";
			int x,d=1;
			for(x=0; x<tablaEncaminamiento.get(key).getMascara()/8; x++){
				mascara += "255";
				if(x<3)
					mascara+= ".";
			}
			if(tablaEncaminamiento.get(key).getMascara()/8<4){
				for(int j=0; j<tablaEncaminamiento.get(key).getMascara()-x*8; j++){
					d = d*2;
				}
				d -=1;
				mascara += d;
				d = 1;
				while(tablaEncaminamiento.get(key).getMascara()/8+d<4){
					mascara += ".0";
					d++;
				}
			}
			try {
				InetAddress mascaraIP = InetAddress.getByName(mascara);
				byte[] mas = mascaraIP.getAddress();
				System.arraycopy(mas, 0, entradas, i, mas.length);
			} catch (UnknownHostException e1) {
				System.err.println("Error en bytes mascara");
			}
			
			
			//IP siguiente salto;
			 i += 4;
			try{
				byte[] dirNextHop = InetAddress.getByName(tablaEncaminamiento.get(key).getNextHop()).getAddress();
				System.arraycopy(dirNextHop, 0, entradas, i, dirNextHop.length);
				i += 4;
			} catch(UnknownHostException e){
				System.err.println("Error en bytes direccion siguiente salto");
			}
			//Falta metrica en bytes
			byte metric = (byte) tablaEncaminamiento.get(key).getNumeroSaltos();
			System.arraycopy(metric, 0, entradas, i+3, 1);
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
		System.out.println("Direccion Destino\tSaltos\tNext Hop\tPuerto");
		Set<String> keys = tablaEncaminamiento.keySet();
		for(String key:keys){
			System.out.println(tablaEncaminamiento.get(key));
		}
	}
	
}
