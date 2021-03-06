/**
 *  TiempoBus - Informacion sobre tiempos de paso de autobuses en Alicante
 *  Copyright (C) 2012 Alberto Montiel
 * 
 *  based on code by ZgzBus Copyright (C) 2010 Francho Joven
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package alberapps.java.tam;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Clase contenedora de la estructura de datos para cada una de las llegadas de
 * un poste
 * 
 * 
 */
public class BusLlegada implements Comparable<BusLlegada> {
	/**
	 * Número de línea
	 */
	private String linea;
	/**
	 * Dirección
	 */
	private String destino;
	/**
	 * Próximo bus en...
	 */
	private String proximo;
	
	
	private String parada;

	/**
	 * Constructor
	 * 
	 * @param linea
	 *            bus
	 * @param destino
	 *            dirección de de destino
	 * @param proximo
	 *            próximo bus en
	 */
	public BusLlegada(String linea, String destino, String proximo, String parada) {
		this.linea = linea;
		this.destino = destino;
		this.proximo = proximo;
		this.parada = parada;
	}

	public int compareTo(BusLlegada bus2) {
		Integer min1 = getProximoMinutos();
		Integer min2 = bus2.getProximoMinutos();
		return min1.compareTo(min2);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((destino == null) ? 0 : destino.hashCode());
		result = prime * result + ((linea == null) ? 0 : linea.hashCode());
		result = prime * result + ((this.getProximoMinutos() == null) ? 0 : getProximoMinutos().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BusLlegada other = (BusLlegada) obj;
		if (destino == null) {
			if (other.destino != null)
				return false;
		} else if (!destino.equals(other.destino))
			return false;
		if (linea == null) {
			if (other.linea != null)
				return false;
		} else if (!linea.equals(other.linea))
			return false;
		if (proximo == null) {
			if (other.proximo != null)
				return false;
		} else if (!proximo.equals(other.proximo))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return linea + " Dirección: " + destino + " en " + proximo;
	}

	/**
	 * @return the linea
	 */
	public String getLinea() {
		return linea;
	}

	/**
	 * @param linea
	 *            the linea to set
	 */
	public void setLinea(String linea) {
		this.linea = linea;
	}

	/**
	 * @return the destino
	 */
	public String getDestino() {
		return destino;
	}

	/**
	 * @param destino
	 *            the destino to set
	 */
	public void setDestino(String destino) {
		this.destino = destino;
	}

	/**
	 * @return the proximo
	 */
	public String getProximo() {
		return proximo;
	}

	/**
	 * @param proximo
	 *            the proximo to set
	 */
	public void setProximo(String proximo) {
		this.proximo = proximo;
	}

	public Integer getProximoMinutos() {
		Integer minutos = 1000;

		String[] procesa = this.proximo.split(";");

		if (procesa[0].equals("enlaparada")) {
			minutos = 0;
		} else if (procesa[0].equals("sinestimacion")) {
			minutos = 9999;
		} else {
			Pattern p = Pattern.compile("([0-9]+) min.");
			Matcher m = p.matcher(procesa[0]);
			if (m.find()) {
				minutos = Integer.valueOf(m.group(1));
			}
		}

		return minutos;
	}
	
	public Integer getSiguienteMinutos() {
		Integer minutos = 1000;

		String[] procesa = this.proximo.split(";");

		if (procesa[1].equals("enlaparada")) {
			minutos = 0;
		} else if (procesa[1].equals("sinestimacion")) {
			minutos = 9999;
		} else {
			Pattern p = Pattern.compile("([0-9]+) min.");
			Matcher m = p.matcher(procesa[1]);
			if (m.find()) {
				minutos = Integer.valueOf(m.group(1));
			}
		}

		return minutos;
	}

	public String getParada() {
		return parada;
	}

	public void setParada(String parada) {
		this.parada = parada;
	}

}
