package paquete;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.Set;

public class Router {
	private String direccionLocal;
	private LinkedHashMap<String, FilaTabla> tablaEncaminamiento;
	private File ficheroConf;
	private int puerto;
	private DatagramSocket socket;
	
	public Router(){
		this(5512);
	}
	
	public Router(int puerto){
		try {
			direccionLocal = InetAddress.getLocalHost().toString().trim().split("/")[1];
			ficheroConf = new File("ripconf-"+direccionLocal+".topo");
		} catch (UnknownHostException e) {
			System.err.println("Error en la captura de la direccion IP");
		}
		tablaEncaminamiento = new LinkedHashMap<String, FilaTabla>();
		this.puerto = puerto;
		try {
			socket = new DatagramSocket(this.puerto);
		} catch (SocketException e) {
			System.err.println("Error en la apertura de socket");
			System.exit(0);
		}
		leerFichero();
	}
	
	private void leerFichero(){
		try {
			Scanner entrada = new Scanner(ficheroConf);
			while(entrada.hasNextLine()){
				String aux = entrada.nextLine();
				if(aux.trim().split("/").length>1){
					tablaEncaminamiento.put(aux.trim().split("/")[0], new FilaTabla(aux.trim().split("/")[0], 0, null, 0));
				}
				else{
					
				}
			}
			entrada.close();
		} catch (FileNotFoundException e) {
			System.err.println("Error en fichero "+ficheroConf.getName());
			System.exit(0);
		}
	}

	/**
	 * Este metodo inicializa dos hilos de ejecución un que envie los datos de la tabla a los routers adyacentes y otro que recibira
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
	 * La función retorna el paquete a enviar con la tabla de encaminamiento
	 * En este paquete no esta implementado la cryptografia ni se considera que el paquete pueda contener mas de 25 entradas el limite del paquete RIP
	 * @return
	 */
	
	public DatagramPacket getPaquete(){
		byte[] cabecera = new byte[3];	//Los bytes 2 y 3 son bytes sin uso
		cabecera[0] = (byte) 0;			//El 1º byte va a ser un 0 porque es una respuesta
		cabecera[1] = (byte) 2;			//El 2º byte va a ser un 2 por la versión
		//byte[] cabecera = {(byte) 0, (byte) 2, (byte) 0, (byte) 0}; Similar a lo de arriba pero en un paso
		byte[] entradas = new byte[tablaEncaminamiento.size()*4-1];
		Set<String> ips = tablaEncaminamiento.keySet();
		int i=0;
		for(String key: ips){
			entradas[i] = Byte.parseByte(key);
			i++;
			entradas[i] = 0; //Falta incorporar mascara de la subred de destino
			i++;
			entradas[i] = Byte.parseByte(tablaEncaminamiento.get(key).getNextHop());
			i++;
			entradas[i] = (byte) tablaEncaminamiento.get(key).getNumeroSaltos();
			i++;
		}
		byte[] paquete = new byte[cabecera.length+entradas.length];
		System.arraycopy(cabecera, 0, paquete, 0, cabecera.length);
		System.arraycopy(entradas, 0, paquete, cabecera.length, entradas.length);
		return new DatagramPacket(paquete, paquete.length);
	}
	
	public DatagramSocket getSocket(){
		return this.socket;
	}
	
}
