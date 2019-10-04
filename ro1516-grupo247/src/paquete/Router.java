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
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Set;

public class Router {
	private InetAddress direccionLocal;
	private LinkedHashMap<InetAddress, FilaTabla> tablaEncaminamiento;
	private File ficheroConf;
	private int puerto;
	private LinkedHashMap<InetAddress,Router> vecinos;
	private DatagramSocket socket;
	private Calendar ultimoEnvio;
	private String contrasena;
	
	public Router(String contrasena){
		this(5512, null, contrasena);
	}
	
	public Router(int puerto, String ip, String Contrasena){
		if(ip==null){
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
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Error en la captura de la direccion IP");
				System.exit(0);
			}
		}
		else{
			try{
				direccionLocal = InetAddress.getByName(ip);
			} catch(UnknownHostException e){
				System.err.println("Error");
				System.exit(0);
			}
		}
		ficheroConf = new File("ripconf-"+direccionLocal.getHostAddress()+".topo");
		tablaEncaminamiento = new LinkedHashMap<InetAddress, FilaTabla>();
		vecinos = new LinkedHashMap<InetAddress, Router>();
		try {
			socket = new DatagramSocket(puerto, this.direccionLocal);
		} catch (SocketException e) {
			System.err.println("Error en socket");
		}
		this.puerto = puerto;
		this.contrasena = Contrasena;
		leerFichero();
		imprimirTabla();
	}
	
	public Router(InetAddress direccion, int puerto){
		this.direccionLocal = direccion;
		this.puerto = puerto;
		ultimoEnvio = Calendar.getInstance();
	}
		
	private void leerFichero(){
		try {
			Scanner entrada = new Scanner(ficheroConf);
			while(entrada.hasNextLine()){
				String aux = entrada.nextLine();
				if(aux.trim().split("/").length>1){
					InetAddress destino = null;
					try{
						destino = InetAddress.getByName(aux.trim().split("/")[0]);
					} catch(UnknownHostException e){
						System.err.println("Error en IPs del fichero");
						System.exit(0);
					}
					tablaEncaminamiento.put(destino, new FilaTabla(destino, 1, direccionLocal, Integer.parseInt(aux.trim().split("/")[1])));
				}
				else if(aux.trim().split(":").length==2){
					try{
						vecinos.put(InetAddress.getByName(aux.trim().split(":")[0]), new Router(InetAddress.getByName(aux.trim().split(":")[0]),
								Integer.parseInt(aux.trim().split(":")[1])));
					}catch(UnknownHostException e){
						System.err.println("Error leer fichero direccion vecinos");
					}
				}
				else{
					try{
						vecinos.put(InetAddress.getByName(aux.trim().split(":")[0]), new Router(InetAddress.getByName(aux.trim().split(":")[0]), 5512));
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

	public void start(){
		int tiempo = (int) (7 + 6*Math.random());
		byte[] datosRecibidos = new byte[25*5*4+4];
		DatagramPacket paqueteRecibido = new DatagramPacket(datosRecibidos, datosRecibidos.length);
		Calendar a = Calendar.getInstance();
		
		while(true){
			try {
				if(tiempo<=0)
					tiempo = 1;
				this.socket.setSoTimeout(1000*tiempo); 
				socket.receive(paqueteRecibido);
				Calendar b = Calendar.getInstance();
				if(vecinos.get(paqueteRecibido.getAddress())==null){
					tiempo = tiempo-(int)(b.getTimeInMillis()/1000-a.getTimeInMillis()/1000);
					continue;
				}
				if(!comprobarContrasena(paqueteRecibido)){
					tiempo = tiempo-(int)(b.getTimeInMillis()/1000-a.getTimeInMillis()/1000);
					continue;
				}
				LinkedList<FilaTabla> lista = actualizarTabla(paqueteRecibido);
				if(lista.size()!=0){
					Set<InetAddress> keys =  vecinos.keySet();
					for(InetAddress key : keys){
						DatagramPacket paqueteEnvio = new DatagramPacket(getPaquete(lista, key), getPaquete(lista, key).length,
								key, vecinos.get(key).getPuerto());
						try {
							socket.send(paqueteEnvio);
						} catch (IOException e1) {
							System.err.println("Error en envio de datos");
							System.exit(0);
						}
					}
				}
				actualizarTabla(paqueteRecibido);
				tiempo = tiempo-(int)(b.getTimeInMillis()/1000-a.getTimeInMillis()/1000);
			} catch (SocketTimeoutException e){
				ComprobarVecinos();
				imprimirTabla();
				Set<InetAddress> keys =  vecinos.keySet();
				for(InetAddress key : keys){
					DatagramPacket paqueteEnvio = new DatagramPacket(getPaquete(key), getPaquete(key).length, key, vecinos.get(key).getPuerto());
					try {
						socket.send(paqueteEnvio);
					} catch (IOException e1) {
						System.err.println("Error en envio de datos");
						System.exit(0);
					}
				}	
				tiempo = (int) (7 + 6*Math.random());
			} catch (IOException e) {
				System.err.println("Error en envio o escucha datos");
				System.exit(0);
			}
			
			a = Calendar.getInstance();
		}
	}
	
	public LinkedList<FilaTabla> actualizarTabla(DatagramPacket paquete){
		int i=24;
		LinkedList<FilaTabla> retorno = new LinkedList<FilaTabla>();
		InetAddress origen = paquete.getAddress();
		vecinos.get(origen).actualizarHoraEnvio();
		while(i<paquete.getData().length){
			if(Byte.toUnsignedInt(paquete.getData()[i+1])!=2)
				break;
			i += 4; //Esta parte es para quitarte el Addres Family y Route Tag
			byte[] direccion = {paquete.getData()[i], paquete.getData()[i+1],paquete.getData()[i+2], paquete.getData()[i+3]};
			InetAddress direccionDestino = null;
			try {
				direccionDestino = InetAddress.getByAddress(direccion);
			} catch (UnknownHostException e) {
				System.err.println("Error direccion destino en actualizar paquete");
			}
			i += 4;
			int masc1 = Byte.toUnsignedInt(paquete.getData()[i]), masc2 = Byte.toUnsignedInt(paquete.getData()[i+1]), masc3 = Byte.toUnsignedInt(paquete.getData()[i+2]), 
				masc4 = Byte.toUnsignedInt(paquete.getData()[i+3]);
			int mascara = Integer.bitCount(masc1) + Integer.bitCount(masc2) + Integer.bitCount(masc3) + Integer.bitCount(masc4);
			i += 11;
			int metrica = Byte.toUnsignedInt(paquete.getData()[i]);
			i++;
			if(direccionDestino.equals(this.direccionLocal))
				continue;
			if(metrica >= 15){
				continue;
			}
			if(tablaEncaminamiento.get(direccionDestino)==null){
				retorno.add(new FilaTabla(direccionDestino, metrica+1, origen, mascara));
				tablaEncaminamiento.put(direccionDestino, new FilaTabla(direccionDestino, metrica+1, origen, mascara));
			}
			if(tablaEncaminamiento.get(direccionDestino)!=null)
				tablaEncaminamiento.get(direccionDestino).actualizarRecepcion();
			if(tablaEncaminamiento.get(direccionDestino).comparar(metrica+1, origen)){
				tablaEncaminamiento.get(direccionDestino).setNextHop(origen);
				tablaEncaminamiento.get(direccionDestino).setNumeroSaltos(metrica+1);
				tablaEncaminamiento.get(direccionDestino).actualizarRecepcion();
				retorno.add(new FilaTabla(direccionDestino, metrica+1, origen, mascara));
			}
		}
		return retorno;
	}

	public void ComprobarVecinos(){
		Calendar horaActual = Calendar.getInstance();
		Set<InetAddress> keys = vecinos.keySet();
		Set<InetAddress> keys2 = tablaEncaminamiento.keySet();
		LinkedList<InetAddress> borrar = new LinkedList<InetAddress>();
		for(InetAddress key : keys){
			Calendar ultimoEnvio = vecinos.get(key).getUltimoEnvio();
			if(horaActual.getTimeInMillis()-ultimoEnvio.getTimeInMillis()>30*1000 && horaActual.getTimeInMillis()-ultimoEnvio.getTimeInMillis()<60*1000){ //En esta parte hay que poner la ruta como inalcanzable 16 saltos
				for (InetAddress key2 : keys2){
					if(tablaEncaminamiento.get(key2).getNextHop().equals(key)){
						tablaEncaminamiento.get(key2).setNumeroSaltos(16);
					}
				}
			}
			else if(horaActual.getTimeInMillis()-ultimoEnvio.getTimeInMillis()>= 60*1000){
				borrar.add(key);
			}
		}
		for(int i=0; i<borrar.size(); i++){
			vecinos.remove(borrar.get(i));
		}
		for(InetAddress key : keys2){
			if(tablaEncaminamiento.get(key).getNextHop().equals(this.direccionLocal))
				continue;
			if(horaActual.getTimeInMillis()-tablaEncaminamiento.get(key).getUltimaActualizacion().getTimeInMillis()>30000){
				tablaEncaminamiento.get(key).setNumeroSaltos(16);
			}
			if(horaActual.getTimeInMillis()-tablaEncaminamiento.get(key).getUltimaActualizacion().getTimeInMillis()>60000){
				borrar.add(key);
			}
		}
		for(int i=0; i< borrar.size(); i++){
			if(tablaEncaminamiento.remove(borrar.get(i))!=null)
				tablaEncaminamiento.remove(borrar.get(i));
		}
	}
	
	public boolean comprobarContrasena(DatagramPacket paquete){
		byte[] contrasena = new byte[this.contrasena.getBytes().length];
		System.arraycopy(paquete.getData(), 8, contrasena, 0, this.contrasena.length());
		String aux ="";
		for(int i=0; i< contrasena.length; i++){
			aux += (char) contrasena[i];
		}
		return aux.equals(this.contrasena);
	}
	
	public byte[] getPaquete(InetAddress destino){
		LinkedList<FilaTabla> lista = new LinkedList<FilaTabla>();
		Set<InetAddress> keys = tablaEncaminamiento.keySet();
		lista.add(new FilaTabla(this.direccionLocal, 0, this.direccionLocal, 32));
		for(InetAddress key : keys){
			lista.add(tablaEncaminamiento.get(key));
		}
		return getPaquete(lista, destino);
	}
	
	public byte[] getPaquete(LinkedList<FilaTabla> lista, InetAddress destino){
		byte[] cabecera = {(byte) 2, (byte) 2, (byte) 0, (byte) 0};
		byte[] entradas = new byte[5*4*lista.size()];
		byte[] autentificacion = new byte[20];
		autentificacion[0] = (byte) 255;
		autentificacion[1] = (byte) 255;
		autentificacion[3] = (byte) 2;
		byte[] password = this.contrasena.getBytes();
		System.arraycopy(password, 0, autentificacion, 4, password.length);
		
		int i = 0;
		if(lista.size()==0)
			return cabecera;
		for(int j = 0; j < lista.size(); j++){
			byte[] AFRT = {(byte) 0, (byte) 2, (byte) 0, (byte) 0};
			System.arraycopy(AFRT, 0, entradas, i, AFRT.length);
			i += 4;
			byte[] dirDestino  = lista.get(j).getDireccionDestino().getAddress();
			System.arraycopy(dirDestino, 0, entradas, i, dirDestino.length);
			i += 4;
			//Mascara
			int mas = 0xFFFFFFFF;
			mas = mas << (32-lista.get(j).getMascara());
			byte[] masc = new byte[4];
			masc[3] = (byte) mas;
			mas = mas >> 8;
			masc[2] = (byte) mas;
			mas = mas >> 8;
			masc[1] = (byte) mas;
			mas = mas >> 8;
			masc[0] = (byte) mas;
			System.arraycopy(masc, 0, entradas, i, masc.length);
			i += 4;

			//IP siguiente salto, esta debera ser 0.0.0.0 se debera coger del paquete la direccion origen;
			byte[] dirNextHop = {(byte) 0, (byte) 0, (byte) 0, (byte) 0};
			System.arraycopy(dirNextHop, 0, entradas, i, dirNextHop.length);
			i += 4;
			//Implementado Split Horizon + poison reverse
			byte[] metric = { (byte) 0, (byte) 0, (byte) 0, (byte) lista.get(j).getNumeroSaltos()};
			if(lista.get(j).getNextHop().equals(destino))
				metric[3] = (byte) 16;
			System.arraycopy(metric, 0, entradas, i, metric.length);
			i += 4;
		}
		byte[] paquete = new byte[cabecera.length+entradas.length+autentificacion.length];
		System.arraycopy(cabecera, 0, paquete, 0, cabecera.length);
		System.arraycopy(autentificacion, 0, paquete, cabecera.length, autentificacion.length);
		System.arraycopy(entradas, 0, paquete, cabecera.length+autentificacion.length, entradas.length);
		return paquete;
	}
	
	public int getPuerto(){
		return this.puerto;
	}
	
	public void actualizarHoraEnvio(){
		this.ultimoEnvio = Calendar.getInstance();
	}
	
	public Calendar getUltimoEnvio(){
		return this.ultimoEnvio;
	}
	
	public LinkedHashMap<InetAddress, FilaTabla> getTabla(){
		return this.tablaEncaminamiento;
	}

	public void imprimirTabla(){
		System.out.println("Direccion Destino\tSaltos\tNext Hop\t\tMascara");
		System.out.println(this.direccionLocal.getHostAddress()+"\t\t0\t\t-\t\t32");
		Set<InetAddress> keys = tablaEncaminamiento.keySet();
		for(InetAddress key:keys){
			if(tablaEncaminamiento.get(key).getNextHop().equals(this.direccionLocal)){
				System.out.println(key.getHostAddress()+"\t\t"+tablaEncaminamiento.get(key).getNumeroSaltos()+"\t\t-\t\t"+
						tablaEncaminamiento.get(key).getMascara());
			}
			else
				System.out.println(tablaEncaminamiento.get(key));
		}
		System.out.println("\n\n");
	}
	
	public void imprimirVecinos(){
		System.out.println("Direccion IP\t\tPuerto");
		Set<InetAddress> keys = vecinos.keySet();
		for(InetAddress key : keys){
			System.out.println(key.getHostAddress()+"\t"+puerto);
		}
	}
}
