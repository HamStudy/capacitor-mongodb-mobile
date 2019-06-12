
  Pod::Spec.new do |s|
    s.name = 'CapacitorMongodbMobile'
    s.version = '0.0.1'
    s.summary = 'MongoDB Mobile plugin for capacitor'
    s.license = 'MIT'
    s.homepage = 'https://github.com/hamstudy/capacitor-mongodb-mobile'
    s.author = 'Richard Bateman <richard@hamstudy.org>'
    s.source = { :git => 'https://github.com/hamstudy/capacitor-mongodb-mobile', :tag => s.version.to_s }
    s.source_files = 'ios/Plugin/**/*.{swift,h,m,c,cc,mm,cpp}'
    s.ios.deployment_target  = '11.0'
    s.dependency 'Capacitor'
  end