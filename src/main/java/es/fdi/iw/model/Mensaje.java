package es.fdi.iw.model;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
//import javax.persistence.OneToMany;
import javax.persistence.ManyToMany;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.validation.constraints.NotNull;

@Entity
@NamedQueries({
    @NamedQuery(name="allMensajes",
            query="select m from Mensaje m"),
    @NamedQuery(name="buscaMensaje", 
     		query="select m from Mensaje m where m.titulo like :nombreParam"),
    @NamedQuery(name="mensajesEntrada",
			query="select m from Mensaje m where m.destinos = :destinoParam"),
    @NamedQuery(name="mensajesSalida",
    		query="select m from Mensaje m where m.origen = :origenParam"),
    @NamedQuery(name="allOrdinario",
    		query="select m from Mensaje m where m.tipo= :ordinarioParam"),
    @NamedQuery(name="allDenuncias", 
    		query="select m from Mensaje m where m.tipo = :denunciaParam"),
    @NamedQuery(name="delUsuarios", 
    		query="delete from Mensaje m where m.origen = :userParam OR m.destinos = :userParam"),
    @NamedQuery(name="delMensaje", 
    		query="delete from Mensaje m where m.id = :idMensaje"),
    @NamedQuery(name="unMensaje", 
    		query="select m from Mensaje m where m.id = :idMensaje"),
    @NamedQuery(name="delDenuncia", 
    		query="delete from Mensaje m where m.id = :idParam"),
    @NamedQuery(name="buscarSolicitud", 
    		query="select m from Mensaje m where m.origen.id = :origen AND m.destinos.id = :destino AND m.tipo = :tipo"),
    @NamedQuery(name="buscarNoLeidos", 
    		query="select m from Mensaje m where m.leido = false AND m.destinos.id = :destino")
})
public class Mensaje {
	private long id;
	private Usuario origen;
	private Usuario destinos;
	private String titulo;
	private String contenido;
	private String tipo;
	private boolean leido;


	public static StringBuilder getJSONString(List<Mensaje> l){
		StringBuilder sb = new StringBuilder("[");
		
		for (Mensaje m : l) {
			if (sb.length()>1) sb.append(",");
			sb.append(getSingleString(m));
		}
		
		return sb;
	}
	
	public static String getSingleString(Mensaje m){
		return "{ "
				+ "\"id\": \"" + m.getId() + "\", "
				+ "\"leido\": \"" + m.getLeido() + "\", "
				+ "\"titulo\": \"" + m.getTitulo() + "\", "
				+ "\"tipo\": \"" + m.getTipo() + "\", "
				+ "\"contenido\": \"" + m.getContenido() + "\", "
				+ "\"idorigen\": \"" + m.getOrigen().getId() + "\", "
				+ "\"iddestino\": \"" + m.getDestinos().getId() + "\", "
				+ "\"nombredestinos\": \"" + m.getDestinos().getNombre() + "\", "
				+ "\"nombreorigen\": \""+m.getOrigen().getNombre()+"\"}";
	}
	
	public static Mensaje crearMensaje(String titulo, String contenido, String tipo, 
			Usuario u, Usuario destino, boolean leido){
		Mensaje m = new Mensaje();
		
		m.titulo = titulo;
		m.contenido = contenido;
		m.tipo = tipo;
		m.origen= u;
		m.destinos = destino;
		m.leido=leido;
		
		return m;
	}

	@Id
    @GeneratedValue
    public long getId() {
      return id;
    }
	public void setId(long id) {
	  this.id = id;
	}

	public String getTitulo() {
      return titulo;
	}
	public void setTitulo(String titulo) {
	  this.titulo = titulo;
	}
	
	@OneToOne(targetEntity=Usuario.class, orphanRemoval=true)//----------?
	public Usuario getOrigen() {
		return origen;
	}
	public void setOrigen(Usuario origen) {
		this.origen = origen;
	}
	
	@OneToOne(targetEntity=Usuario.class , orphanRemoval=true)
	public Usuario getDestinos() {
		return destinos;
	}
	public void setDestinos(Usuario destinos) {
		this.destinos = destinos;
	}
	public String getContenido() {
		return contenido;
	}
	public void setContenido(String contenido) {
		this.contenido = contenido;
	}
	public String getTipo() {
		return tipo;
	}
	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public boolean getLeido() {
		return leido;
	}

	public void setLeido(boolean leido) {
		this.leido = leido;
	}
}