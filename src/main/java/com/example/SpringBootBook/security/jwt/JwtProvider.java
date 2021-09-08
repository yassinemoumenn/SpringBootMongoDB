package com.example.SpringBootBook.security.jwt;

import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.SpringBootBook.security.UserPrincipal;
import com.example.SpringBootBook.util.SecurityUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class JwtProvider  implements IJwtProvider{
	
	
	 @Value("${app.jwt.secret}")
	    private String JWT_SECRET;

	    @Value("${app.jwt.expiration-in-ms}")
	    private Long JWT_EXPIRATION_IN_MS;
	    
	    
//	TO USE USER INFO AND CREAT JWT TOKEN
	@Override
	public String generateToken(UserPrincipal auth) {
		String authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .setSubject(auth.getUsername())
                .claim("roles", authorities)
                .claim("userId", auth.getId())
                .setExpiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION_IN_MS))
                .signWith(SignatureAlgorithm.HS512, JWT_SECRET)
                .compact();
	}
//  TO READ AUTHOROZATION HEADER FROM HTTPSERVLETREQUEST AND CREATE AUTHENTICATION OBJECT
	@Override
	public Authentication getAuthentication(HttpServletRequest request) {
		
		 Claims claims = extractClaims(request);

	        if (claims == null)
	        {
	            return null;
	        }

	        String username = claims.getSubject();
	        Long userId = claims.get("userId", Long.class);

	        Set<GrantedAuthority> authorities = Arrays.stream(claims.get("roles").toString().split(","))
	                .map(SecurityUtils::convertToAuthority)
	                .collect(Collectors.toSet());

	        UserDetails userDetails = UserPrincipal.builder()
	                .username(username)
	                .authorities(authorities)
	                .id(userId)
	                .build();

	        if (username == null)
	        {
	            return null;
	        }
	        return new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
	    }
	 private Claims extractClaims(HttpServletRequest request)
	    {
	        String token = SecurityUtils.extractAuthTokenFromRequest(request);

	        if (token == null)
	        {
	            return null;
	        }

	        return Jwts.parser()
	                .setSigningKey(JWT_SECRET)
	                .parseClaimsJws(token)
	                .getBody();
	    }
	//	TO READ AUTHORIZATION HEADER FROM HTTPSERVLETREQUEST AND VALIDATE IT
    @Override
    public boolean validateToken(HttpServletRequest request)
    {
        Claims claims = extractClaims(request);

        if (claims == null)
        {
            return false;
        }

        if (claims.getExpiration().before(new Date()))
        {
            return false;
        }
        return true;
    }
}
