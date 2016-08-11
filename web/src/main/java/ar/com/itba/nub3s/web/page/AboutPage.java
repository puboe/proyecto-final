package ar.com.itba.nub3s.web.page;

import java.io.File;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.model.Model;

@SuppressWarnings("serial")
public class AboutPage extends AbstractWebPage {

	@Override
	protected void onInitialize() {
		super.onInitialize();
		add(new Label("tutor1", Model.of("Ignacio Alvarez-Hamelin - ihameli@itba.edu.ar ")));
		add(new Label("creator1", Model.of("Díaz Uboe, Pablo - pdiazubo@itba.edu.ar ")));
		add(new Label("creator2", Model.of("Elli, Federico - felli@itba.edu.ar ")));
		add(new Label("creator3", Model.of("Pomar, Federico - fpomar@itba.edu.ar ")));
		add(new Label("brief", Model.of("Para poder dibujar las flechas, se utiliza una variación del algoritmo de pareo de bloques,"
				+ " del cual se obtiene una secuencia de puntos en el plano. Se dibujan las flechas utilizando estos puntos, una flecha azul"
				+ " por cada bloque apareado, y una flecha roja con menor frecuencia. De esta manera las flechas azules marcan una trayectoria,"
				+ " mientras que las flechas rojas sirven para resaltar la dirección.")));
		add(new Label("blueExplanation", Model.of(
				"Las flechas azules indican la dirección y el sentido en la que se mueven las nubes. Por lo tanto indican la dirección"
				+ " en la que soplan los distintos vientos. A medida que se aumenta la cantidad de pasos, se dibujan más flechas"
				+ " para poder así apreciar el trayecto que recorre la nube a lo largo de la animación."
				+ " Adicionalmente se utiliza transparencia para que en caso de superponerse, se pueda visualizar con mayor opacidad, dónde hay"
				+ " vientos con mayor intensidad.")));
		add(new Label("redExplanation", Model.of(
				"Las flechas rojas se muestran donde hay mayor desplazamiento, por lo tanto se visualizan con menor frecuencia. Adicionalmente"
				+ " son de mayor tamaño. Son de color rojo con el fin de que se pueda visualizar mejor y contrasten contra las flechas azules."
				+ " Para obtener los mejores resultados recomendamos seleccionar entre 5 y 15 pasos, de manera tal que no falte, ni se sobrecargue"
				+ " de información.<br/><br/>Para más detalles sobre el proyecto, puede descargar el informe completo haciendo clink en el botón inferior,"
				+ " en dónde, en el capítulo 4, sección 1.2 (4.1.2), se explica con detalle el proceso que se realiza.")).setEscapeModelStrings(false));
		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource("informe.pdf").getFile());
		add(new DownloadLink("pdfDownloadLink", file, "informe.pdf"));
	}
}

