package paquete;

import java.net.InetAddress;
import java.net.UnknownHostException;

//Falta añadir mascara de subred

public class FilaTabla {
	private String direccionDestino;
	private InetAddress direccion;
	private int numeroSaltos;
	private String nextHop;
	private int puertoEnvio;
	public String mascaraSubRed;
	
	public FilaTabla(String direccionDestino, int numeroSaltos, String nextHop, int puertoEnvio, String mascaraSubRed){
		this.direccionDestino = direccionDestino;
		try {
			direccion = InetAddress.getByName(direccionDestino);
		} catch (UnknownHostException e) {
			System.err.println("Error en Direccion destino");
		}
		this.numeroSaltos = numeroSaltos;
		this.nextHop = nextHop;
		this.puertoEnvio = puertoEnvio;
		this.mascaraSubRed = mascaraSubRed;
	}
	
	public void actualizarInformacion(int numeroSaltos, String nextHop, int puertoEnvio){
		if(this.numeroSaltos>numeroSaltos){
			this.numeroSaltos = numeroSaltos;
			this.nextHop = nextHop;
			this.puertoEnvio = puertoEnvio;
		}
	}
	
	public InetAddress getDireccion(){
		return this.direccion;
	}
	
	public String getDireccionDestino(){
		return this.direccionDestino;
	}
	
	public int getNumeroSaltos(){
		return this.numeroSaltos;
	}
	
	public int getPuerto(){
		return this.puertoEnvio;
	}
	
	public String getNextHop(){
		return this.nextHop;
	}
	
	public String getMascaraSubRed(){
		return this.mascaraSubRed;
	}
	
	public String toString(){
		return direccionDestino+"\t\t"+numeroSaltos+"\t"+nextHop+"\t"+puertoEnvio;
	}
}
