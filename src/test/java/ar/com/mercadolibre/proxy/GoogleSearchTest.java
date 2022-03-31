package ar.com.mercadolibre.proxy;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

public class GoogleSearchTest {
	
	private WebDriver driver;
	
	@Before
	public void setUp() {
		System.setProperty("webdriver.chrome.driver", "./src/test/resources/chromedriver/chromedriver.exe");
		this.driver=new ChromeDriver();
		
		this.driver.manage().window().maximize();
		this.driver.get("https://www.google.com.ar");
	}
	
	@Test
	public void googleTestPage() {
		WebElement searchBox=this.driver.findElement(By.name("q"));
		searchBox.clear();
		searchBox.sendKeys("pepe soplakenas");
		searchBox.submit();
		this.driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
	}

}
