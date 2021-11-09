## How to use

### 1、config
	repositories {
		... ...
    		maven { url 'https://jitpack.io' }
	}
	
	dependencies {
		... ...
    		implementation 'com.github.fldyown:universe:0.0.2'
	}
	
### 2、simple
	1、config scan package
	
	@ComponentScan({"com.fldy", "com.fldy.simple"})
	public class Config {}


	2、add annotation
	
	@Component
	public class EarthService {
	    public void create() {
	        System.out.println("create earth : " + System.currentTimeMillis());
	    }
	
	    public void destroy() {
	        System.out.println("destroy earth : " + System.currentTimeMillis());
	    }
	}
	
	@Component("universeService")
	@Scope("prototype")
	public class UniverseService {
	
	    @Autowired
	    EarthService earthService;
	
	    public void createEarth() {
	        earthService.create();
	    }
	
	    public void destroyEarth() {
	        earthService.destroy();
	    }
	}
	
	
	3、if need
	
	@Component
	public class AppProcessor implements UniverseProcessor {
	    @Override
	    public Object before(String name, Object o) {
	        System.out.println("before:" + name + " " + o);
	        return o;
	    }
	
	    @Override
	    public Object after(String name, Object o) {
	        System.out.println("after:" + name + " " + o);
	        return o;
	    }
	}


### init and use 

	 /**
         * init container
         */
        Universe.init(Config.class);
        /**
         * get service
         */
        UniverseService us = Universe.get(UniverseService.class);
        /**
         * use
         */
        us.createEarth();
        us.destroyEarth();
        
## For All Jvm Projects
	
	