package paquete;

import java.net.InetAddress;
import java.net.UnknownHostException;

//Falta añadir mascara de subred

public class FilaTabla {
	private InetAddress direccionDestino;
	private int numeroSaltos;
	private InetAddress nextHop;
	public int mascaraSubRed;
	
	public FilaTabla(InetAddress direccionDestino, int numeroSaltos, InetAddress nextHop, int mascaraSubRed){
		this.direccionDestino = direccionDestino;
		this.nextHop = nextHop;
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
	
	public void setNumeroSaltos(int saltos){
		this.numeroSaltos = saltos;
	}
	
	public int getNumeroSaltos(){
		return this.numeroSaltos;
	}
	
	public void setNextHop(InetAddress nextHop){
		this.nextHop = nextHop;
	}
	
	public InetAddress getNextHop(){
		return this.nextHop;
	}
	
	public int getMascara(){
		return this.mascaraSubRed;
	}
	
	public String toString(){
		return direccionDestino+"\t\t"+numeroSaltos+"\t"+nextHop.getHostAddress()+"\t\t"+mascaraSubRed;
	}
	
	public boolean comparar(int numeroSaltos, InetAddress nextHop){
		if(numeroSaltos<this.numeroSaltos){
			this.nextHop = nextHop;
			return true;
		}
		return false;
	}
}
