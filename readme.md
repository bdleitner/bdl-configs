# Config
*Configuration classes with Dependency Injection for Java*
**Benjamin Leitner**

### Configurables
-----
`Configurable<T>` is a bucket-like class that simply holds a value.
The value can be updated either by directly setting a new `Object`
value or by setting a `String` value that is parsed into an `Object`
of the correct type.

They can have a default value or not, but in the latter case a `Class`
object must be provided so the Configurable can have information about
its type:

    Configurable.value("some_string"); // A Configurable<String> with default value "some_string". 
    Configurable.noDefault(String.class); // A Configurable<String> with null default value.

Configurables can also be made writable only until they are read, for
use as Flags.  To do this, use:

    // A Configurable<String> with default value "some_string"*[]: 
    // After reading the value, attempting to set it will throw an exception.
    // Setting the value to something else prior to calling get() is allowed.
    Configurable.flag("some_string");
    
    // Similar, but the default value is null.
    Configurable.noDefaultFlag(String.class);

For classes other than primitives and strings, a *Parser* must be
provided so the Configurable can translate new string values into objects.

    // A Configurable for a custom class
    Configurable.<MyClass>builder()
        .withDefaultValue(myInstance)
        .withParser(myClassParser) // A com.google.common.base.Function<String, MyClass>
        .build();

### Dependency Injection
-----
The real magic comes into play when using Configurables with Dependency Injection. Adding the
annotation `@Config` to a configurable tells the included annotation processor to process the
Configurable field. The annotation processor generates modules for both the *Dagger* and *Guice*
DI frameworks.

How the `@Config`-marked Configurables are grouped into modules depends on their visibility.
public and private `@Config` fields are pulled up into a root Module class in the lowest package
that includes all subpackages with at least one `@Config`-marked Configurable.  Package-local fields
are included in modules in the same package, and these modules are linked to the root module.

The annotation processor will write one or more modules, with a Root module appearing in the highest
level package that contains all `@Config`-marked configurables itself or in subpackages.

By default, the modules will bind the configuration value to an `@ConfigValue([name])` annotation,
where `name` is the value given in the `@Config` annotation, if present, or the field name of the
Configurable otherwise.  Note that these bindings must be globally unique.  To change these
bindings, a `@Qualifer` (for *Dagger*) and/or `@BindingAnnotation` (for *Guice*) can also be placed
on the Configurable field.  If one is found, the binding is replaced in the corresponding DI module.
That is, if a `@Qualifier` annotation is found, the binding is overridden in *Dagger*.  If a
`@BindingAnnotation` is found, the binding is overridden in *Guice*.  These potential overrides are
independent.  To allow for use of either, use an annotation that is both a `@Qualifer` and a
`@BindingAnnotation` (like `@ConfigValue`).

#### Guice
To use *Guice* for dependency injection, include in your `Injector` creation both the
`MainConfigGuiceModule` and any generated `ConfigGuiceModule`s needed e.g:

    public static void main(String[] args) {
      Injector injector = Guice.createInjector(
          MaingConfigGuiceModule.create(),
          new path.to.package.one.ConfigGuiceModule(),
          new some.other.library.path.ConfigGuiceModule(),
          ...
          );
      injector.getInstance(...);
    }


    
You can then include `@ConfigValue([name])`-annotated parameters in your constructors and they will
be filled appropriately.

#### Dagger
To use *Dagger* for dependency injection, create a Component that includes `MainConfigDaggerModule`
 and any generated `ConfigDaggerModule`s needed e.g:

    @Component(modules = {
        MainConfigDaggerModule.class,
        path.to.package.one.ConfigDaggerModule.class,
        some.other.library.path.ConfigDaggerModule.class})
    public interface ConfigEnabledComponent {
    }

and then build it as follows:

    public static void main(String[] args) {
       ConfigEnabledComponent component = DaggerConfigEnabledComponent.builder()
           .mainConfigDaggerModule(MainConfigDaggerModule.create())
           .build()
    }

#### Notes
1. You need not install all of the modules (if more than one) generated from one annotation
 processor run.  The modules generated at one time will have a single root module that includes
 all the others.
2. If you are not using Configurables as flags and expect the values may change, it is highly
  recommended to inject `@ConfigValue([name]) Provider<[Type]>` rather than
  `@ConfigValue([name]) Type`. 
   
#### Values from Command Line Arguments
The initial values for Configurables can also be set from the command line (or from any list of
strings).  To do this, in the code above simply replace:

    MainConfig(Guice|Dagger)Module.create()
    
with:

    MainConfig(Guice|Dagger)Module.forArguments([args])

where `[args]` can be a list of strings or a varargs String array.  The rules of processing are:

* Each string represents a single config assignment.
* An assignment has the form `--[config_name]=[config_value]`.  The config name can be either
the `name` as defined above, or the FQPN of the field can be used.  If more than one config has
the same `name`, then FQPN must be used.
* For boolean configs, the `=[config_value]` may be omitted.  Including `--[config_name]` will set
the value to `true` and `--no[config_name]` will set it to `false`.
* If a string `--` is encountered, all further processing of values terminates.
* There are three special config names that indicate other places to load config values
  * `config_file` - the value for this argument is interpreted as a file from which to read more
  config name-value pairs.
  * `config_resource` - the value is treated as the URL of a Resource from which to read more
  config name-value pairs.
  * `system_config` - the value is a comma-separated list of names of System Properties from which
  to read values.
 