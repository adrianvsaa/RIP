package paquete;

import java.net.InetAddress;
import java.util.Calendar;

//Falta a�adir mascara de subred

public class FilaTabla {
	private InetAddress direccionDestino;
	private int numeroSaltos;
	private InetAddress nextHop;
	private int mascaraSubRed;
	private Calendar ultimaActualizacion;
	
	
	public FilaTabla(InetAddress direccionDestino, int numeroSaltos, InetAddress nextHop, int mascaraSubRed){
		this.direccionDestino = direccionDestino;
		this.nextHop = nextHop;
		this.numeroSaltos = numeroSaltos;
		this.mascaraSubRed = mascaraSubRed;
		this.ultimaActualizacion = Calendar.getInstance();
	}
	
	public void actualizarRecepcion(){
		this.ultimaActualizacion = Calendar.getInstance();
	}
	
	public Calendar getUltimaActualizacion(){
		return this.ultimaActualizacion;
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
		return direccionDestino.getHostAddress()+"\t\t"+numeroSaltos+"\t"+nextHop.getHostAddress()+"\t\t"+mascaraSubRed;
	}
	
	public boolean comparar(int numeroSaltos, InetAddress nextHop){
		if(numeroSaltos<this.numeroSaltos){
			return true;
		}
		return false;
	}
	
}
