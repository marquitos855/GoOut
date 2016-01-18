package es.fdi.iw.controller;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.swing.JOptionPane;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import es.fdi.iw.ContextInitializer;
import es.fdi.iw.model.Actividad;
import es.fdi.iw.model.Comentario;
import es.fdi.iw.model.Foro;
import es.fdi.iw.model.Hito;
import es.fdi.iw.model.Mensaje;
import es.fdi.iw.model.Registro;
import es.fdi.iw.model.Tag;
import es.fdi.iw.model.Usuario;

/**
 * Handles requests for the application home page.
 */
@Controller
public class HomeController {
	
	private static final Logger logger = LoggerFactory.getLogger(HomeController.class);
	@PersistenceContext
	private EntityManager entityManager;
	
	/** MÉTODO PARA EL REGISTRO */
		
	@RequestMapping(value = "/registro", method = RequestMethod.POST)
	@Transactional
	public String registro(
			@RequestParam("login") String formLogin,
			@RequestParam("pass") String formPass,
			@RequestParam("pass2") String formPass2,
			@RequestParam("email") String formEmail,
			HttpServletRequest request, HttpServletResponse response, 
			Model model, HttpSession session) {
		
		logger.info("Login attempt from '{}' while visiting '{}'", formLogin);
		
		
		// validate request
		if (formLogin == null || formLogin.length() < 3 || formPass == null || formPass.length() < 4) 
		{
			model.addAttribute("loginError", "Usuarios y contraseña: 4 caracteres mínimo");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} 
		else 
		{
			Usuario u = null;
			try {
				u = (Usuario)entityManager.createNamedQuery("userByLogin")
					.setParameter("loginParam", formLogin).getSingleResult();
				
			} catch (NoResultException nre) {
				if (formPass.equals(formPass2)) 
				{
					logger.info("no-such-user; creating user {}", formLogin);				
					Usuario user = Usuario.createUser(formLogin, formPass, "usuario", "Sin especificar",null, "Sin especificar", formEmail, "");

					entityManager.persist(user);
				} 
				else {
					logger.info("no such login: {}", formLogin);
				}
				model.addAttribute("loginError", "Error en usuario o contraseña");
			}
		}
		
		// redireccion a login cuando el registro ha sido correcto
		return "redirect:login";
	}
	
	
	/** MÉTODO PARA EL LOGIN **/
	
	@RequestMapping(value = "/login", method = RequestMethod.POST)
	@Transactional
	public String login(
			@RequestParam("login") String formLogin,
			@RequestParam("pass") String formPass,
			String destino,
			HttpServletRequest request, HttpServletResponse response, 
			Model model, HttpSession session) {
		
		logger.info("Login attempt from '{}' while visiting '{}'", formLogin);
		destino="login";
		
				if (formLogin == null || formLogin.length() < 3 || formPass == null || formPass.length() < 3) {
					model.addAttribute("loginError", "Usuarios y contraseña: 3 caracteres mínimo");
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				} else {
					Usuario u = null;
					try {
						u = (Usuario)entityManager.createNamedQuery("userByLogin")
							.setParameter("loginParam", formLogin).getSingleResult();
						if (u.isPassValid(formPass)) {
							model.addAttribute("loginError","pass valido");
							logger.info("pass valido");
							session.setAttribute("usuario", u);
							getTokenForSession(session);
							destino = "redirect:home";
						} else {
							logger.info("pass no valido");
							model.addAttribute("loginError", "Error en usuario o contraseña");
							response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
						}
					} catch (NoResultException nre) {
						
						model.addAttribute("loginError", "Error en usuario o contraseña");
						destino = "login";
					}
				}

				return destino;
			}
	
	/**
	 *  MÉTODO Logout. 
	 *  Elimina la sesion actual y cierra sesion redirigiendo a la pantalla de login.
	 */
	
	@RequestMapping(value = "/logout", method = RequestMethod.GET)
	public String logout(HttpSession session) {
		logger.info("Usuario '{}' logged out", session.getAttribute("usuario"));
		session.invalidate();
		return "redirect:login";
	}
	
	/**
	 *	Metodo donde se pueden modificar los datos del usuario una vez esta logueado.
	 *	Además se puede agregar informacion como provincia, fecha de nacimiento etc.
	 */
	
	
	@RequestMapping(value = "/modificarActividad", method = RequestMethod.POST)
	@Transactional
	public String modificarActividad(
			@RequestParam("nombre_actividad") String nombre,
			@RequestParam("lugar") String lugar,
			@RequestParam("num_participantes") int nparticipantes,
			@RequestParam("idactividad") long idactividad,
			@RequestParam("fecha_inicio") Date fecha_ini,
			@RequestParam("fecha_fin") Date fecha_fin){
		
		Actividad a = null;
		a = (Actividad) entityManager.createNamedQuery("unaActividad")
				.setParameter("actividadParam", idactividad).getSingleResult();
		
		a.setNombre(nombre);
		a.setLocalizacion(lugar);
		a.setMaxPersonas(nparticipantes);
		a.setFecha_ini(fecha_ini);
		a.setFecha_fin(fecha_fin);
		entityManager.persist(a);
		
		return "redirect:actividad/"+idactividad;
	}
	
	@RequestMapping(value = "/mi_perfil", method = RequestMethod.POST)
	@Transactional
	public String modPerfil(
			@RequestParam("nick_perfil") String nick,
			@RequestParam("prov_perfil") String provincia,
			@RequestParam("email_perfil") String email,
			@RequestParam("photo") MultipartFile foto,
			HttpServletRequest request, HttpServletResponse response, 
			Model model, HttpSession session) {
		
			Usuario usuario = (Usuario)session.getAttribute("usuario");
			String imagen ="";
			try {
	        	if(!foto.isEmpty()){
	        		//String nombre_imagen = foto.getOriginalFilename();
					//extension = nombre_imagen.substring(nombre_imagen.lastIndexOf("."),nombre_imagen.length());
					
	        		imagen = usuario.getId()+"";
				
				byte[] bytes = foto.getBytes();
	            BufferedOutputStream stream =
	                    new BufferedOutputStream(
	                    		new FileOutputStream(ContextInitializer.getFile("usuario", imagen)));
	            stream.write(bytes);
	            stream.close();
	        }
			}
	        catch(Exception e){
	        	
	        	//Error
	        	
	        }
		
			if(session.getAttribute("usuario")!=null)
			{
				Usuario u = null;
				try {
									
					u = (Usuario)entityManager.createNamedQuery("userByLogin")
						.setParameter("loginParam", nick).getSingleResult();
					
					u.setProvincia(provincia);
					u.setEmail(email);
					
					entityManager.persist(u);
					
					session.setAttribute("usuario", u);
					getTokenForSession(session);
					


			} catch (NoResultException nre) {
		
					//Error
			}
		
				// redireccion a login cuando el registro ha sido correcto
				return "redirect:mi_perfil";
			}
		else
			return "sin_registro";
	}
	
	@ResponseBody
	@RequestMapping(value="/usuario/imagen", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
	public byte[] usuarioImagen(@RequestParam("id") String id) throws IOException {
	    File f = ContextInitializer.getFile("usuario", id);
	    InputStream in = null;
	    if (f.exists()) {
	    	in = new BufferedInputStream(new FileInputStream(f));
	    } else {
	    	in = new BufferedInputStream(
	    			this.getClass().getClassLoader().getResourceAsStream("no_imagen.jpg"));
	    }
	    
	    return IOUtils.toByteArray(in);
	}
	
	@RequestMapping(value = "/mensajeAmigo", method = RequestMethod.GET)
	@Transactional
	public String mensajeAmigo(@RequestParam("nombre_amigo") long amigo, Model model, HttpSession session){
		Usuario p=entityManager.find(Usuario.class, amigo);
		
		model.addAttribute("nombre", p);
		//session.setAttribute("nombre", p);
		//getTokenForSession(session);
		//return "redirect:mensajes";
		return "mensajes";
	}
	
	@RequestMapping(value= "/denunciarActividad")
	@Transactional
	public String denunciarActividad(@RequestParam("id_actividad") long actv, Model model, HttpSession session){
		Mensaje m = null;
		Usuario u = null;
		Usuario d = null;
		Actividad a = null;
		
		String asunto;
		String contenido;
		
		u=(Usuario)session.getAttribute("usuario");
		a = (Actividad) entityManager.createNamedQuery("unaActividad")
				.setParameter("actividadParam", actv).getSingleResult();
		
		asunto = "Denuncia actividad";
		//contenido = "La actividad ha sido denunciada";
		contenido = "El usuario " + u.getLogin() + "(" + u.getId() + ")" + 
		" ha denunciado la actividad " + a.getNombre() + "(" + a.getId()
		+ ")." + "Entre paréntesis el id de cada elemento respectivamente";
		
		m = Mensaje.crearMensaje(asunto, contenido, "denuncia", d, d);
		entityManager.persist(m);
		
		return "redirect:actividad/"+actv;
		//return "actividad";
	}
	/*----------------------------------------operaciones administrador---------------------------*/
	
	
	//borrados--------------------------------
	

	//adds-------------------------------------
	
	
	@RequestMapping(value = "/addActividad", method = RequestMethod.POST)
	@Transactional
	public String addActividad(
			@RequestParam("nombre_actividad") String nombre_actv,
			@RequestParam("fecha_inicio") Date fecha_ini,
			@RequestParam("fecha_fin") Date fecha_fin,
			@RequestParam("lugar") String origen,
			@RequestParam("destino") String destino,
			@RequestParam("num_participantes") int max_participantes,
			Model model, HttpSession session){
		
		Actividad a = null;
		Registro r = null;
		Usuario usuario_creador = null;
		String privacidad="publica";
		
		usuario_creador = entityManager.find(Usuario.class,((Usuario)session.getAttribute("usuario")).getId());
		a = Actividad.crearActividad(nombre_actv,max_participantes,usuario_creador, fecha_ini, fecha_fin, origen, destino, privacidad);
		r = Registro.crearRegistro(a, usuario_creador);
		Foro f=Foro.crearForo();
		a.setForo(f);
		
		entityManager.persist(f);
		entityManager.persist(a);
		a.getRegistros().add(r);
		
		entityManager.persist(usuario_creador);
		usuario_creador.getRegistros().add(r);
		
		entityManager.persist(r);
		
		
		return "redirect:mis_actividades";
	}

	@RequestMapping(value = "/crearActividad", method = RequestMethod.POST)
	@Transactional
	public String crearActividad(
			@RequestParam("nombre_actv") String nombre_actv,
			@RequestParam("max_participantes") int max_participantes,
			@RequestParam("imagen") MultipartFile imagen_actv,
			@RequestParam("tags") long[] tagIds,
			@RequestParam("fecha_ini") Date fecha_ini,
			@RequestParam("fecha_fin") Date fecha_fin,
			@RequestParam("origen") String origen,
			@RequestParam("destino") String destino,
			@RequestParam("actv_privada") int privado,
			Model model, HttpSession session) throws IOException {

			if(session.getAttribute("usuario")!=null){
			Actividad a = null;
			Registro r = null;
			Usuario usuario_creador = null;
			String imagen="";
			String extension="";
			String privacidad="publica";
				
			if(privado == 1)
				privacidad = "privada";
			
			try {

				usuario_creador = entityManager.find(Usuario.class,((Usuario)session.getAttribute("usuario")).getId());
				a = Actividad.crearActividad(nombre_actv,max_participantes,usuario_creador, fecha_ini, fecha_fin, origen, destino, privacidad);
				r = Registro.crearRegistro(a, usuario_creador);
				Foro f=Foro.crearForo();
				a.setForo(f);
				
				entityManager.persist(f);
				entityManager.persist(a);
				a.getRegistros().add(r);
				
				entityManager.persist(usuario_creador);
				usuario_creador.getRegistros().add(r);
				
				entityManager.persist(r);	
				
				for (long aid : tagIds) {
					// adding authors to book is useless, since author is the owning side (= has no mappedBy)
					Tag t = entityManager.find(Tag.class, aid);
					t.getEtiquetados().add(a);
					entityManager.persist(t);
				}
				

			        try {
			        	if(imagen_actv.isEmpty())
			        		imagen="0.png";
			        	else{
			        		String nombre_imagen = imagen_actv.getOriginalFilename();
							extension = nombre_imagen.substring(nombre_imagen.lastIndexOf("."),nombre_imagen.length());
							
			        		imagen = a.getId()+extension;
						
						byte[] bytes = imagen_actv.getBytes();
		                BufferedOutputStream stream =
		                        new BufferedOutputStream(
		                        		new FileOutputStream(ContextInitializer.getFile("actv", imagen)));
		                stream.write(bytes);
		                stream.close();}
			        }
			        catch(Exception e){
			        	
			        	//Error
			        	
			        }


				a.setIdImagen(imagen);
				
			
				
				
			} catch (NoResultException nre) {
	
				//Error
			}

				return "redirect:mis_actividades";
			}
			else{
				return "sin_registro";
			}
		}
	
	
	@RequestMapping(value = "/crearTag", method = RequestMethod.POST)
	@Transactional
	public String crearTag(
			@RequestParam("nombre_tag") String nombre_tag){
		
		Tag t = null;
		
		t = Tag.crearTag(nombre_tag);
		
		entityManager.persist(t);
		
		return "redirect:crear";
	}

	@RequestMapping(value = "/hacerComentario", method = RequestMethod.POST)
	@Transactional
	public String hacerComentario(@RequestParam("asunto") String asunto, @RequestParam("actividad") long actividad, HttpSession session){
		
		Actividad a = entityManager.find(Actividad.class,actividad);
		
		Comentario c=Comentario.crearComentario(asunto, ((Usuario)session.getAttribute("usuario")));
		
		a.getForo().getComentarios().add(c);

		
		entityManager.persist(c);
		entityManager.persist(a);
		
		return "redirect:actividad/"+actividad;
	}
	
	@RequestMapping(value = "/crearHito", method = RequestMethod.POST)
	@Transactional
	public String crearHito(@RequestParam("actividad") long actividad,	@RequestParam("fecha") Date fecha,  @RequestParam("asunto") String asunto){
		
		Actividad a=entityManager.find(Actividad.class, actividad);
		
		Hito h= Hito.crearHito(asunto, fecha);
		
		if(a.getHitos()==null){
			List<Hito> lHitos=new ArrayList<Hito>();
			a.setHitos(lHitos);
		}
		a.getHitos().add(h);
		
		entityManager.persist(h);
		entityManager.persist(a);

		
		return "redirect:actividad/"+actividad;
	}
	
	
	
	@RequestMapping(value = "/crearMensaje", method = RequestMethod.POST)
	@Transactional
	public String crearMensaje(
			@RequestParam("asunto") String titulo,
			@RequestParam("destinatario") String destino,
			@RequestParam("mensaje") String contenido,
			@RequestParam("tipo") String tipo,
			HttpServletRequest request, HttpServletResponse response, 
			Model model, HttpSession session){
			
		
			Mensaje m = null;
			Usuario u = null; // El usuario que manda el mensaje
			Usuario d = null; // Destinatario
			
			try{
				u=(Usuario)session.getAttribute("usuario");
				
				d = (Usuario)entityManager.createNamedQuery("userByLogin")
						.setParameter("loginParam", destino).getSingleResult();
				
				//d = entityManager.find(Usuario.class, destino);
				
				
				m = Mensaje.crearMensaje(titulo, contenido, tipo,u, d);
				entityManager.persist(m);
			}
			catch(NoResultException nre){
				//ERROR
			}
		if(tipo.equals("ordinario"))	
			return "redirect:mensajes?metodo=salida";
		else	
			return "redirect:actividad/"+contenido;
	}
	
	@RequestMapping(value = "/solicitudAmigo", method = RequestMethod.POST)
	@Transactional
	public String solicitudAmigo(
		@RequestParam("id_amigo") long amigo,
		HttpSession session){
		
		Usuario usuario_amigo = null;
		Usuario usuario_propio = null;
		Usuario d = null; // Destinatario
		
		Mensaje m = null;
		String titulo="Solicitud de amistad";

		try{
			usuario_propio=(Usuario)session.getAttribute("usuario");
			usuario_amigo = entityManager.find(Usuario.class,amigo);
			
			d = (Usuario)entityManager.createNamedQuery("userByLogin").setParameter("loginParam", usuario_amigo.getLogin()).getSingleResult();
			
			String contenido = usuario_propio.getLogin()+" quiere ser tu amigo";
			
			m = Mensaje.crearMensaje(titulo, contenido, "solicitud",usuario_propio, d);
			entityManager.persist(m);
		}
		catch(NoResultException nre){
			//ERROR
		}
				
		
			return "redirect:perfil/"+d.getId();
	}


	@RequestMapping(value = "/agregarAmigo", method = RequestMethod.POST)
	@Transactional
	public String agregarAmigo(
		@RequestParam("id_amigo") long amigo,
		@RequestParam("id_propio") long propio,
		HttpSession session){
		
		Usuario usuario_amigo = null;
		Usuario usuario_propio = null;
		int i = 0;
		boolean existe = false;
		
		try
		{
			usuario_amigo = entityManager.find(Usuario.class,amigo);
			usuario_propio = entityManager.find(Usuario.class,propio);
			List<Usuario> aux = usuario_propio.getAmigos();
			
			while(aux.size()> i){
				if(aux.get(i).getId() == usuario_amigo.getId())
					existe=true;
				i++;
			}
			
			if(!existe)
			{
				usuario_propio.getAmigos().add(usuario_amigo);
				usuario_amigo.getAmigos().add(usuario_propio);
				
				entityManager.persist(usuario_propio);
				entityManager.persist(usuario_amigo);
			}
			
		}
		catch(Exception e)
		{
			
		}
		
		
			return "redirect:mi_perfil";
	}
	
	
	/** METODOS PARA ACTIVIDAD **/
	
	//Metodo para unirse a una actividad
	
	@RequestMapping(value = "/unirseActividad", method = RequestMethod.POST)
	@Transactional
	public String unirseActividad(
			@RequestParam("id_actv") long id_actividad,
			@RequestParam("id_propio") long id_propio,
			HttpSession session){
		if(session.getAttribute("usuario")!=null){
		
		Actividad actv = new Actividad();
		Usuario usuario = new Usuario();
		Registro r = null;
		int i = 0;
		boolean existe = false;
		
		actv = entityManager.find(Actividad.class, id_actividad);
		usuario = entityManager.find(Usuario.class, id_propio);
		
		// Comprobar que la actividad no este cerrada
		
		//if(/*actv.getEstado().equals("cerrada") || actv.getEstado().equals("completa")*/ false){	
			//Aviso al usuario de que no puede unirse a esta actividad. NO seria necesario?
		//}
		//else
		//{
			if(actv.getNpersonas() < actv.getMaxPersonas())
			{
				while(i < actv.getRegistros().size() && !existe){
					if(actv.getRegistros().get(i).getUsuario() == usuario)
						existe = true;
					i++;
				}
				
				if(!existe){
					r = Registro.crearRegistro(actv, usuario);
					
					usuario.getRegistros().add(r);
					entityManager.persist(usuario);
					
					actv.getRegistros().add(r);
					actv.setNpersonas(actv.getNpersonas()+1);
					entityManager.persist(actv);
					
					entityManager.persist(r);
				}
			/*	if(!actv.getPersonas().contains(usuario)){
					//Unirse a la actividad
					usuario.getActuales().add(actv);
					entityManager.persist(usuario);
				
					actv.getPersonas().add(usuario);
					entityManager.persist(actv);
				
					//Incrementar el numero de personas y si es igual a max personas, cerrar (poner completa) la actividad.
					actv.setNpersonas(actv.getPersonas().size());
				}
				*/
			}
			else
			{
				//Aviso al usuario de que no puede unirse a esta actividad por que esta llena
			}
		//}
		
		}
		
		
		return "redirect:actividad/"+id_actividad;
		
	}
	
	
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String home(Locale locale, Model model, HttpSession session) {
		
		if(session.getAttribute("usuario")!=null){
			model.addAttribute("actividades", entityManager.createNamedQuery("allActividades").getResultList());
	
			return "home";
		}else
			return "redirect:sin_registro";
	}
	
	
	@RequestMapping(value = "home", method = RequestMethod.GET)
	public String home(Model model, HttpSession session){
		if(session.getAttribute("usuario")!=null){
			model.addAttribute("actividades", entityManager.createNamedQuery("allActividades").getResultList());
			return "home";
		}else
			return "redirect:sin_registro";
	}
	
	
	@RequestMapping(value = "/crear", method = RequestMethod.GET)
	public String crear(Model model, HttpSession session){
		if(session.getAttribute("usuario")!=null){
			
			HashMap<Integer, Tag> tagMap = new HashMap<Integer, Tag>();
		
			List<Tag> lT=entityManager.createNamedQuery("allTags").getResultList();
			
			 for (Tag t: lT) {
				 tagMap.put(t.getEtiquetados().size(), t);
			 }		
			 
			 Iterator<Tag> iter = (Iterator) tagMap.values().iterator();

			 
			 int i=0;
			 while (iter.hasNext() && i<10) {
			        lT.set(i, (Tag)iter.next());
			        i++;
			 } 
			 
			 
			 model.addAttribute("tags", lT);
			model.addAttribute("usuarios", entityManager.createNamedQuery("allUsers").getResultList());
			return "crear";
		}else
			return "redirect:sin_registro";
	}
	
	@RequestMapping(value="/filtrarActividades", method = RequestMethod.GET)
	@Transactional
	public String filtrarActividades(@RequestParam("filtro") int filtro, Model model, HttpSession session){
		
		if(((Usuario)session.getAttribute("usuario"))!=null){
			
			Usuario u = (Usuario)session.getAttribute("usuario");
			List<Actividad> actividades = new ArrayList<Actividad>();
			u = entityManager.find(Usuario.class, u.getId());
			List <Registro> r=u.getRegistros();
			
			
			if(filtro == 5)
				model.addAttribute("actividades", entityManager.createNamedQuery("actividadesCreadas").setParameter("creadorParam", u).getResultList());
			else{
				for(int i=0; i<r.size();i++){
					actividades.add(r.get(i).getActividad());
				}
				
				model.addAttribute("actividades",actividades);
			}
					
			return "mis_actividades";
		}
		else
			return "redirect:sin_registro";
		
		/*if(filtro == 5)
			System.out.println("-------------------------" + filtro);
		if(filtro == 4)
			System.out.println("-------------------------" + filtro);
		if(filtro == 3)
			System.out.println("-------------------------" + filtro);
		if(filtro==2)
			System.out.println("-------------------------" + filtro);
		if(filtro==1)
			System.out.println("-------------------------" + filtro);*/
		
	}

	@RequestMapping(value = "/mis_actividades", method = RequestMethod.GET)
	public String mis_actividades(Model model, HttpSession session){
		
		
		if(((Usuario)session.getAttribute("usuario"))!=null){
	
			return "mis_actividades";
		}
		else
			return "redirect:sin_registro";
	}
	
	
	@RequestMapping(value = "/salirActividad", method = RequestMethod.POST)
	@Transactional
	public String salirActividad(@RequestParam("actividad") long actividad, Model model,HttpSession session){
		
		Actividad a = entityManager.find(Actividad.class, actividad);
		
		a.setNpersonas(a.getNpersonas()-1);
		
		entityManager.persist(a);
		entityManager.createNamedQuery("delRegistro").setParameter("actividadParam",actividad).setParameter("usuarioParam", ((Usuario) session.getAttribute("usuario")).getId()).executeUpdate();
		
		
		
		return "redirect:home";
	}
	
	@RequestMapping(value = "/actividad/{id}", method = RequestMethod.GET)
	public String actividad(@PathVariable("id") long id,HttpServletResponse response,Model model,HttpSession session){
		Actividad a = entityManager.find(Actividad.class, id);
		boolean pertenece=true;
		
		
		if (a == null) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			logger.error("No such actividad: {}", id);
		} else {
			List <Usuario> participantes=new ArrayList();
			
			Usuario u=(Usuario)session.getAttribute("usuario");
			
			try{
				if(u==null)throw new NoResultException();
				
				List<Registro> lr=entityManager.createNamedQuery("pertenece").setParameter("actividadParam",id).setParameter("usuarioParam", u.getId()).getResultList();
			
				if(lr.isEmpty())throw new NoResultException();
				
			}catch(NoResultException nre){
				pertenece=false;
			}
			
			
			
			for(int i=0; i<a.getRegistros().size(); i++){
				participantes.add(a.getRegistros().get(i).getUsuario());
			
			}
			
			model.addAttribute("hitos", a.getHitos());
			model.addAttribute("comentarios", a.getForo().getComentarios());
			model.addAttribute("pertenece", pertenece);
			model.addAttribute("participantes", participantes);
			model.addAttribute("actividad", a);
			model.addAttribute("tags", a.getTags());
			model.addAttribute("encuestas", a.getEncuestas());
		}
		model.addAttribute("prefix", "../");
		return "actividad";
	}
	
	@RequestMapping(value = "/buscar", method = RequestMethod.GET)
	public String buscar(Model model,HttpSession session){
		if(session.getAttribute("usuario")!=null){
		
			model.addAttribute("actividades", entityManager.createNamedQuery("allActividades").getResultList());
			
			return "buscar";
		}else
			return "redirect:sin_registro";
	}
	
	@RequestMapping(value = "/buscarAmigos", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<String> buscarAmigos(@RequestParam("buscado") String buscado,@RequestParam("tipo") String tipo,HttpServletResponse response,Model model,HttpSession session){
		buscado="%"+buscado+"%";
		List<Usuario> usuarios =null;
		usuarios= entityManager.createNamedQuery("buscaUsuario").setParameter("loginParam", buscado).getResultList();		
		Usuario u=((Usuario)session.getAttribute("usuario"));
	
		
		StringBuilder sb = new StringBuilder("[");
		
		if(tipo.equals("misamigos")){
			sb=Usuario.getJSONString(u.getAmigos());	
		}else{
			if(tipo.equals("noamigos")){
				List<Usuario> no_amigos=new ArrayList<Usuario>();
				
				boolean amigos=false;
				for(Usuario us: usuarios){
					for(int i=0; i<u.getAmigos().size() && !amigos; i++){
						amigos=(us.getId()==usuarios.get(i).getId() || us.getId()==u.getId());
					}
					if(!amigos) no_amigos.add(us);
					amigos=false;
				}
				
				sb=Usuario.getJSONString(no_amigos);
			}else{
				sb=Usuario.getJSONString(usuarios);
			}
		}

		return new ResponseEntity<String>(sb + "]", HttpStatus.OK);
	}
	
	@RequestMapping(value = "/buscarActividades", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<String> buscarActividades(@RequestParam("buscado") String buscado,@RequestParam("tipo") String tipo, HttpSession session){
		List<Actividad> buscadas = null;
		Usuario u=(Usuario)session.getAttribute("usuario");
		
		buscado="%"+buscado+"%";
		
		buscadas = entityManager.createNamedQuery("buscaActividad").setParameter("nombreParam", buscado).getResultList();

		List<Actividad> mis_actividades =new ArrayList<Actividad>();

		if(!u.getRegistros().isEmpty())
			for(Registro r: u.getRegistros())
				mis_actividades.add(r.getActividad());
		
		StringBuilder sb = new StringBuilder("[");
		
		if(tipo=="misactividades"){

			sb=Actividad.getJSONString(mis_actividades);
		}else{
			if(tipo.equals("nomias")){	
				List<Actividad> no_mias=new ArrayList<Actividad>();
				
				boolean mia=false;
				for(Actividad a: buscadas){
					for(int i=0; i<mis_actividades.size() && !mia; i++){
						mia=(a.getId()==mis_actividades.get(i).getId());
					}
					if(!mia) no_mias.add(a);
					mia=false;
				}
				
				sb=Actividad.getJSONString(no_mias);
			}else{
				sb=Actividad.getJSONString(buscadas);
			}
			
		}
	
		return new ResponseEntity<String>(sb + "]", HttpStatus.OK);	
	}
	
	@RequestMapping(value = "/actividad_creador", method = RequestMethod.GET)
	public String actividad_creador(){
		return "actividad_creador";
	}
	
	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public String login(){
		return "login";
	}
	
	@RequestMapping(value = "/perfil/{id}", method = RequestMethod.GET)
	public String perfil(@PathVariable("id") long id,HttpServletRequest request, HttpServletResponse response, 
			Model model, HttpSession session){
		
		boolean amigos = false;
		model.addAttribute("usuarios", entityManager.createNamedQuery("allUsers").getResultList());
		model.addAttribute("prefix", "../");
		
		if(session.getAttribute("usuario")!=null){
		
			Usuario p=entityManager.find(Usuario.class, id);
			Usuario u=(Usuario)session.getAttribute("usuario");
			
			for (Usuario buscado : u.getAmigos()) {
				if(buscado.getId()==id)
						amigos=true;
			}
			
			
			model.addAttribute("amigos", amigos);
		
			if (p == null) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				logger.error("No such perfil: {}", id);
			} else {
				model.addAttribute("perfil", p);
			}
			return "perfil";
		}
		else
		{
			return "redirect:../sin_registro";
		}
		
	}

	
	@RequestMapping(value = "/mi_perfil", method = RequestMethod.GET)
	public String mi_perfil(HttpSession session, Model model){
		if(session.getAttribute("usuario")!=null){
			Usuario u=(Usuario)session.getAttribute("usuario");
			
			u = entityManager.find(Usuario.class,u.getId());
			
			model.addAttribute("amigos", u.getAmigos());
			
			return "mi_perfil";
		}else 
			return "redirect:sin_registro";
	}
	
	@RequestMapping(value = "/mensajes", method = RequestMethod.GET)
	public String mensajes(Model model, HttpSession session){
		if(session.getAttribute("usuario")!=null){
			Usuario u = null;
			u=(Usuario)session.getAttribute("usuario");
			model.addAttribute("mensajes", entityManager.createNamedQuery("mensajesEntrada").setParameter("destinoParam", u).getResultList());
			model.addAttribute("mensajeS", entityManager.createNamedQuery("mensajesSalida").setParameter("origenParam", u).getResultList());
			model.addAttribute("usuario", u);
			return "mensajes";
		}
		else return "redirect:sin_registro";
	}
	
	@RequestMapping(value = "/registro", method = RequestMethod.GET)
	public String registro(){
		return "registro";
	}
	
	@RequestMapping(value = "/sin_registro", method = RequestMethod.GET)
	public String sin_registro(Model model){
		model.addAttribute("actividades", entityManager.createNamedQuery("allActividades").getResultList());
		return "sin_registro";
	}
	
	@RequestMapping(value = "/amigos", method = RequestMethod.GET)
	public String amigos(Model model,HttpSession session){
		if(session.getAttribute("usuario")!=null){
		
			Usuario u=(Usuario)session.getAttribute("usuario");
			model.addAttribute("usuario", u);			
			model.addAttribute("usuarios", entityManager.createNamedQuery("allUsers").getResultList());
			
			return "amigos";
		}else
			return "redirect:sin_registro";
	}
	
	
	@RequestMapping(value = "/borrarElemento", method = RequestMethod.POST)
	@ResponseBody
	@Transactional
	public void borrarElemento(@RequestParam("id") long [] id, @RequestParam("tipo") String tipo, HttpServletRequest request){
		try {
			
			if(tipo.equals("Actividad")){
				for(int i = 0; i < id.length; i++){
					Actividad a = entityManager.find(Actividad.class, id[i]);
					for(int j = 0; j < a.getRegistros().size(); j++){
						Registro r = entityManager.find(Registro.class, a.getRegistros().get(j).getId());
						entityManager.createNamedQuery("delRegistro").setParameter("idRegistro", r.getId()).executeUpdate();
					}
					entityManager.createNamedQuery("eliminarActividad").setParameter("idActividad", a.getId()).executeUpdate();
				}
			}else{
				if(tipo.equals("Usuario")){
					for(int i = 0; i < id.length; i++){
						Usuario u = entityManager.find(Usuario.class, id[i]);
						for(int j = 0; j < u.getRegistros().size(); j++){
							Registro r = entityManager.find(Registro.class, u.getRegistros().get(j).getId());
							entityManager.createNamedQuery("delRegistro").setParameter("idRegistro", r.getId()).executeUpdate();
						}
						
						for(int j = 0; j < u.getAmigos().size(); j++){
							Usuario us = entityManager.find(Usuario.class, u.getAmigos().get(j).getId());
							us.getAmigos().remove(u);
							entityManager.persist(us);
						}
						u.getAmigos().removeAll(u.getAmigos());
						entityManager.createNamedQuery("delUsuario").setParameter("idUsuario", u.getId()).executeUpdate();
					}
				}else{	
				
					for(int i=0; i<id.length; i++)
						entityManager.createNamedQuery("del"+tipo).setParameter("id"+tipo, id[i]).executeUpdate();
			
					
				}	
			}
		} catch (NoResultException nre) {
			logger.error("No such"+ tipo + ": {}", id);
		}
	
	}
	
	
	
	@RequestMapping(value = "/administrador", method = RequestMethod.GET)
	@Transactional
	public String administrador(Model model, HttpSession session){
		
		Usuario u = (Usuario)session.getAttribute("usuario");
		
		if(u.getRol().equals("admin")){
			
			model.addAttribute("actividades", entityManager.createNamedQuery("allActividades").getResultList());
			model.addAttribute("mensajes", entityManager.createNamedQuery("allOrdinario").setParameter("ordinarioParam", "ordinario").getResultList());
			model.addAttribute("usuarios", entityManager.createNamedQuery("allUsers").getResultList());
			model.addAttribute("tags", entityManager.createNamedQuery("allTags").getResultList());
			model.addAttribute("novedades", entityManager.createNamedQuery("allNovedades").getResultList());
			model.addAttribute("pagos", entityManager.createNamedQuery("allPagos").getResultList());
			model.addAttribute("hitos", entityManager.createNamedQuery("allHitos").getResultList());
			model.addAttribute("comentarios", entityManager.createNamedQuery("allComentarios").getResultList());
			model.addAttribute("foros", entityManager.createNamedQuery("allForos").getResultList());
			model.addAttribute("denuncias", entityManager.createNamedQuery("allDenuncias").setParameter("denunciaParam", "denuncia").getResultList());
			model.addAttribute("registros", entityManager.createNamedQuery("allRegistros").getResultList());

			return "administrador";
		
		}else{
			if(session.getAttribute("usuario")!=null){
				return "redirect:home";
			}
			else return "redirect:sin_registro";
		}
	}
	

	static String getTokenForSession (HttpSession session) {
	    String token=UUID.randomUUID().toString();
	    session.setAttribute("csrf_token", token);
	    return token;
	}
	
}
