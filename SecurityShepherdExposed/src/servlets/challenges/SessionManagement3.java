package servlets.challenges;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Encoder;

import utils.Hash;
import utils.ShepherdExposedLogManager;
import dbProcs.Database;

/**
 * Session Management Challenge Three
 * <br/><br/>
 * This file is part of the Security Shepherd Project.
 * 
 * The Security Shepherd project is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.<br/>
 * 
 * The Security Shepherd project is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.<br/>
 * 
 * You should have received a copy of the GNU General Public License
 * along with the Security Shepherd project.  If not, see <http://www.gnu.org/licenses/>. 
 * @author Mark Denihan
 *
 */
public class SessionManagement3 extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static org.apache.log4j.Logger log = Logger.getLogger(SessionManagement3.class);
	private static String levelName = "Session Management Challenge Three";
	private static String levelHash = "t193c6634f049bcf65cdcac72269eeac25dbb2a6887bdb38873e57d0ef447bc3";
	private static String levelResult = "e62008dc47f5eb065229d48963";
	/**
	 * Users must use this functionality to sign in as an administrator to retrieve the result key. If the user name is valid but not the passwor, an error message with the username is returned.
	 * @param userName Sub schema user name
	 * @param password Sub schema user password
	 */
	public void doPost (HttpServletRequest request, HttpServletResponse response) 
	throws ServletException, IOException
	{
		//Setting IpAddress To Log and taking header for original IP if forwarded from proxy
		ShepherdExposedLogManager.setRequestIp(request.getRemoteAddr(), request.getHeader("X-Forwarded-For"));
		//Attempting to recover username of session that made request
		try
		{
			if (request.getSession() != null)
			{
				HttpSession ses = request.getSession();
				String userName = (String) ses.getAttribute("decyrptedUserName");
				log.debug(userName + " accessed " + levelName + " Servlet");
			}
		}
		catch (Exception e)
		{
			log.debug(levelName + " Servlet Accessed");
			log.error("Could not retrieve username from session");
		}
		PrintWriter out = response.getWriter();  
		out.print(getServletInfo());
		Encoder encoder = ESAPI.encoder();
		String htmlOutput = new String();
		log.debug(levelName + " Servlet Accessed");
		try
		{
			log.debug("Getting Challenge Parameters");
			Object nameObj = request.getParameter("subUserName");
			Object passObj = request.getParameter("subUserPassword");
			String subName = new String();
			String subPass = new String();
			String userAddress = new String();
			if(nameObj != null)
				subName = (String) nameObj;
			if(passObj != null)
				subPass = (String) passObj;
			log.debug("subName = " + subName);
			log.debug("subPass = " + subPass);
			
			log.debug("Getting ApplicationRoot");
			String ApplicationRoot = getServletContext().getRealPath("");
			log.debug("Servlet root = " + ApplicationRoot );
			
			Connection conn = Database.getChallengeConnection(ApplicationRoot, "BrokenAuthAndSessMangChalThree");
			log.debug("Checking credentials");
			PreparedStatement callstmt;
			
			log.debug("Commiting changes made to database");
			callstmt = conn.prepareStatement("COMMIT");
			callstmt.execute();
			log.debug("Changes commited.");
			
			callstmt = conn.prepareStatement("SELECT userName, userAddress, userRole FROM users WHERE userName = ?");
			callstmt.setString(1, subName);
			log.debug("Executing findUser");
			ResultSet resultSet = callstmt.executeQuery();
			if(resultSet.next())
			{
				log.debug("User found");
				if(resultSet.getString(3).equalsIgnoreCase("admin"))
				{
					log.debug("Admin Detected");
					callstmt = conn.prepareStatement("SELECT userName, userAddress, userRole FROM users WHERE userName = ? AND userPassword = SHA(?)");
					callstmt.setString(1, subName);
					callstmt.setString(2, subPass);
					log.debug("Executing authUser");
					ResultSet resultSet2 = callstmt.executeQuery();
					if(resultSet2.next())
					{
						log.debug("Successful Admin Login");
						// Get key and add it to the output
						String userKey = Hash.generateUserSolution(levelResult, request.getCookies());
						
						htmlOutput = "<h2 class='title'>Welcome " + encoder.encodeForHTML(resultSet2.getString(1)) + "</h2>" +
								"<p>" +
								"The result key is <a>" + userKey + "</a>" +
								"</p>";
					}
					else
					{
						userAddress = "Incorrect password for <a>" + encoder.encodeForHTML(resultSet.getString(1)) + "</a><br/>";
						htmlOutput = htmlStart + userAddress + htmlEnd;
					}
				}
				else
				{
					log.debug("Successful Guest Login");
					htmlOutput = htmlStart + htmlEnd +
							"<h2 class='title'>Welcome Guest</h2>" +
							"<p>No further information for Guest Users currently available. " +
							"If your getting bored of the current functions available, " +
							"you'll just have to upgrade yourself to an administrator somehow.</p><br/><br/>";	
				}
			}
			else
			{
				userAddress = "Username not found.<br/>";
				htmlOutput = htmlStart + userAddress + htmlEnd;
			}
			Database.closeConnection(conn);
			log.debug("Outputing HTML");
			out.write(htmlOutput);
		}
		catch(Exception e)
		{
			out.write("An Error Occured! You must be getting funky!");
			log.fatal(levelName + " - " + e.toString());
		}
	}
	
	private static String htmlStart = "<table>";
	private static String htmlEnd = "<tr><td>Username:</td><td><input type='text' id='subUserName'/></td></tr>" +
			"<tr><td>Password:</td><td><input type='password' id='subUserPassword'/></td></tr>" +
			"<tr><td colspan='2'><div id='submitButton'><input type='submit' value='Sign In'/>" +
			"</div></td></tr>" +
			"</table>";
}