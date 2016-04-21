package paquete;

import java.net.InetAddress;
import java.net.UnknownHostException;

//Falta añadir mascara de subred

public class FilaTabla {
	private InetAddress direccionDestino;
	private int numeroSaltos;
	private InetAddress nextHop;
	public String mascaraSubRed;
	
	public FilaTabla(String direccionDestino, int numeroSaltos, String nextHop, String mascaraSubRed){
		try {
			this.direccionDestino = InetAddress.getByName(direccionDestino);
			this.nextHop = InetAddress.getByName(nextHop);
		} catch (UnknownHostException e) {
			System.err.println("Error en Direccion destino");
		}
		this.numeroSaltos = numeroSaltos;
		this.mascaraSubRed = mascaraSubRed;
	}
	
	public void actualizarInformacion(int numeroSaltos, String nextHop, int puertoEnvio){
		if(this.numeroSaltos>numeroSaltos){
			this.numeroSaltos = numeroSaltos;
			try{
			this.nextHop = InetAddress.getByName(nextHop);
			}catch(UnknownHostException e){
				System.err.println("Error actualizar información");
			}
		}
	}
	
	public InetAddress getDireccionDestino(){
		return this.direccionDestino;
	}
	
	public int getNumeroSaltos(){
		return this.numeroSaltos;
	}
	
	
	public InetAddress getNextHop(){
		return this.nextHop;
	}
	
	public String getMascaraSubRed(){
		return this.mascaraSubRed;
	}
	
	public int getMascara(){
		return Integer.parseInt(this.mascaraSubRed);
	}
	
	public String toString(){
		return direccionDestino+"\t\t"+numeroSaltos+"\t"+nextHop;
	}
}
