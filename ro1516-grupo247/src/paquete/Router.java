package paquete;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Scanner;

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
}
