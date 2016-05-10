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
			direccionLocal = InetAddress.getLocalHost();
			ficheroConf = new File("ripconf-"+direccionLocal.getHostAddress()+".topo");
		} catch (Exception e) {
			System.err.println("Error en la captura de la direccion IP");
			System.exit(0);
		}
		tablaEncaminamiento = new LinkedHashMap<InetAddress, FilaTabla>();
		vecinos = new LinkedHashMap<InetAddress, Router>();
		try {
			socket = new DatagramSocket(puerto);
		} catch (SocketException e) {
			System.err.println("Error en socket");
		}
		this.puerto = puerto;
		leerFichero();
		imprimirVecinos();
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
					tablaEncaminamiento.put(destino, new FilaTabla(destino, 0, direccionLocal, Integer.parseInt(aux.trim().split("/")[1])));
				}
				else if(aux.trim().split(":").length>1){
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

	/**
	 * Este metodo inicializa dos hilos de ejecución un que envie los datos de la tabla a los routers adyacentes y otro que recibira
	 * las tablas de otros routers y los procesa
	 */
	
	public void start(){
		int tiempo = (int) (7 + 6*Math.random());
		byte[] datosRecibidos = new byte[25*5*4+4];
		DatagramPacket paqueteRecibido = new DatagramPacket(datosRecibidos, datosRecibidos.length);
		Calendar a = Calendar.getInstance();
		
		while(true){
			try {
				System.out.println("Tiempo de espera: "+ tiempo);
				this.socket.setSoTimeout(1000*tiempo); //Metodo que pone un limite de espera a la escucha del socket
				socket.receive(paqueteRecibido);
				System.out.println("Paquete recibido");
				Calendar b = Calendar.getInstance();
				if(isActualizable(paqueteRecibido)){
					System.out.println("Enviando Trigered Update");
					ComprobarVecinos();
					LinkedList<FilaTabla> lista = actualizarTabla(paqueteRecibido);
					actualizarTabla(paqueteRecibido);
					Set<InetAddress> keys =  vecinos.keySet();
					for(InetAddress key : keys){
						DatagramPacket paqueteEnvio = new DatagramPacket(getPaquete(lista, key), getPaquete(lista, key).length,
								key, vecinos.get(key).getPuerto());
						try {
							socket.send(paqueteEnvio);
						} catch (IOException e1) {
							System.err.println("Error en envio de datos");
						}
					}
				}
				tiempo -= (b.getTimeInMillis()*1000-a.getTimeInMillis()*1000);
			} catch (SocketTimeoutException e){
				ComprobarVecinos();
				imprimirVecinos();
				imprimirTabla();
				
				Set<InetAddress> keys =  vecinos.keySet();
				for(InetAddress key : keys){
					DatagramPacket paqueteEnvio = new DatagramPacket(getPaquete(key), getPaquete(key).length, key, vecinos.get(key).getPuerto());
					try {
						System.out.println("Enviando paquete a: "+key.getHostAddress()+"\n\n");
						socket.send(paqueteEnvio);
					} catch (IOException e1) {
						System.err.println("Error en envio de datos");
					}
				}		
			} catch (IOException e) {
				System.err.println("Error en envio o escucha datos");
			}
			tiempo = (int) (7 + 6*Math.random());
			a = Calendar.getInstance();
		}
	}
	
	/**
	 * Empezando a implementar trigered updates
	 * Si queremos utilizar este metodo para implementar el trigered update hay que cambiar el final porque si no modifica ya la tabla
	 * @param paquete
	 * @return
	 */
	
	public boolean isActualizable(DatagramPacket paquete){
		int i = 4;
		boolean cambios = false;
		InetAddress origen = paquete.getAddress();
		if(vecinos.get(origen)==null)
			return true;
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
			i += 15;
			int metrica = Byte.toUnsignedInt(paquete.getData()[i]);
			i++;
			if(tablaEncaminamiento.get(direccionDestino)==null){
				cambios = true;
				break;
			}
			else if(tablaEncaminamiento.get(direccionDestino).comparar(metrica+1, origen)){
				cambios = true;
				break;
			}
		}
		return cambios;
	}
	
	
	public LinkedList<FilaTabla> actualizarTabla(DatagramPacket paquete){
		int i=4;
		LinkedList<FilaTabla> retorno = new LinkedList<FilaTabla>();
		InetAddress origen = paquete.getAddress();
		if(vecinos.get(origen)==null){
			vecinos.put(origen, new Router(origen, paquete.getPort()));
		}
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
			if(metrica>=15)		//Si la metrica anterior es 15 o mayor el destino es inalcanzable y no aparece en la tabla
				continue;
			else if(tablaEncaminamiento.get(direccionDestino)==null){
				retorno.add(new FilaTabla(direccionDestino, metrica+1, origen, mascara));
				tablaEncaminamiento.put(direccionDestino, new FilaTabla(direccionDestino, metrica+1, origen, mascara));
			}
			else if(tablaEncaminamiento.get(direccionDestino).comparar(metrica+1, origen)){
				tablaEncaminamiento.get(direccionDestino).setNextHop(origen);
				tablaEncaminamiento.get(direccionDestino).setNumeroSaltos(metrica+1);
				retorno.add(new FilaTabla(direccionDestino, metrica+1, origen, mascara));
			}
		}
		return retorno;
	}
	
	/**
	 * Metodo que comprueba si los vecinos tardan mas de 30 segundos en enviar un paquete en ese caso se borra ese vecino de la lista
	 * y todos los destinos que van hacia ese vecino
	 */
	
	public void ComprobarVecinos(){
		Calendar horaActual = Calendar.getInstance();
		Set<InetAddress> keys = vecinos.keySet();
		Set<InetAddress> keys2 = tablaEncaminamiento.keySet();
		boolean aux = true;
		InetAddress borrar = null;
		do{
			borrar = null;
			aux = false;
			for(InetAddress key : keys){
				Calendar ultimoEnvio = vecinos.get(key).getUltimoEnvio();
				if(horaActual.getTimeInMillis()-ultimoEnvio.getTimeInMillis()>30*1000){ //En esta parte hay que poner la ruta como inalcanzable 16 saltos
					for (InetAddress key2 : keys2){
						if(tablaEncaminamiento.get(key2).getNextHop().equals(key)){
							tablaEncaminamiento.get(key2).setNumeroSaltos(16);
						}
					}
				}
				else if(horaActual.getTimeInMillis()-ultimoEnvio.getTimeInMillis()>60*1000){
					borrar = key;
					break;
				}
			}
			if(borrar==null)
				break;
			if(horaActual.getTimeInMillis()-vecinos.get(borrar).getUltimoEnvio().getTimeInMillis()>30*1000){
				aux = true;
				vecinos.remove(borrar);
			}
		}while(aux);
		do{
			aux = false;
			borrar = null;
			for(InetAddress key2 : keys2){
				if(vecinos.get(tablaEncaminamiento.get(key2).getNextHop())==null&&
						!tablaEncaminamiento.get(key2).getNextHop().equals(this.direccionLocal)){
					borrar = key2;
					break;
				}
			}
			if(borrar==null)
				break;
			if(vecinos.get(tablaEncaminamiento.get(borrar).getNextHop())==null){
				tablaEncaminamiento.remove(borrar);
				aux = true;
			}
		}while(aux);
	}
	
	
	/**
	 * La función retorna el paquete a enviar con la tabla de encaminamiento
	 * En este paquete no esta implementado la cryptografia ni se considera que el paquete pueda contener mas de 25 entradas el limite del paquete RIP
	 * Empezando a hacer split Horizon + poison reverse
	 */ 
	
	public byte[] getPaquete(InetAddress destino){
		byte[] cabecera = {(byte) 2, (byte) 2, (byte) 0, (byte) 0}; 
		
		byte[] autentificacion = new byte[24];
		autentificacion[0] = (byte) 2;
		autentificacion[1] = (byte) 2;
		autentificacion[4] = (byte) 255;
		autentificacion[5] = (byte) 255;
		autentificacion[7] = (byte) 2;
		//Los 16 octetos restantes son para la contraseña
		
		
		if(tablaEncaminamiento.size()==0)
			return cabecera;
		byte[] entradas = new byte[tablaEncaminamiento.size()*4*5];
		Set<InetAddress> ips = tablaEncaminamiento.keySet();
		int i=0;
		for(InetAddress key: ips){
			//Addres Family y route TAG
			byte[] AFRT = {(byte) 0, (byte) 2, (byte) 0, (byte) 0};
			System.arraycopy(AFRT, 0, entradas, i, AFRT.length);
			i += 4;
			byte[] dirDestino = key.getAddress();
			System.arraycopy(dirDestino, 0, entradas, i, dirDestino.length);
			i += 4;
			//Mascara
			byte[] masc = new byte[4];
			int x,d;
			for(x=0; x<tablaEncaminamiento.get(key).getMascara()/8; x++){
				if(x>=4)
					break;
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
			
			//Implementado Split Horizon + poison reverse
			byte[] metric = { (byte) 0, (byte) 0, (byte) 0, (byte) tablaEncaminamiento.get(key).getNumeroSaltos()};
			if(tablaEncaminamiento.get(key).getNextHop().equals(destino))
				metric[3] = (byte) 16;
			System.arraycopy(metric, 0, entradas, i, metric.length);
			i += 4;
			
		}
		byte[] paquete = new byte[cabecera.length+entradas.length];
		System.arraycopy(cabecera, 0, paquete, 0, cabecera.length);
		System.arraycopy(entradas, 0, paquete, cabecera.length, entradas.length);
		return paquete;
	}
	
	
	public byte[] getPaquete(LinkedList<FilaTabla> lista, InetAddress destino){
		byte[] cabecera = {(byte) 2, (byte) 2, (byte) 0, (byte) 0};
		byte[] entradas = new byte[5*4*lista.size()];
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
			byte[] masc = new byte[4];
			int x,d;
			for(x=0; x<lista.get(j).getMascara()/8; x++){
				if(x>=4)
					break;
				masc[x] = (byte) 255;
			}
			if(lista.get(j).getMascara()/8<4){
				d = (int) (Math.pow(2, lista.get(j).getMascara()-x*8)-1);
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
			//Implementado Split Horizon + poison reverse
			byte[] metric = { (byte) 0, (byte) 0, (byte) 0, (byte) lista.get(j).getNumeroSaltos()};
			if(lista.get(j).getNextHop().equals(destino))
				metric[3] = (byte) 16;
			System.arraycopy(metric, 0, entradas, i, metric.length);
			i += 4;
		}
		byte[] paquete = new byte[cabecera.length+entradas.length];
		System.arraycopy(cabecera, 0, paquete, 0, cabecera.length);
		System.arraycopy(entradas, 0, paquete, cabecera.length, entradas.length);
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
		Set<InetAddress> keys = tablaEncaminamiento.keySet();
		for(InetAddress key:keys){
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
