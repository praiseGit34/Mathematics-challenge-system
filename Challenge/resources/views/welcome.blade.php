<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Mathematics Challenge System</title>
   
    <link href="https://maxcdn.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css" rel="stylesheet">
    
    <link rel="stylesheet" href="./resources/css/style.css">
    <style>
       
body {
    font-family: Arial, sans-serif;
    background-color: #f0f0f0;
    margin: 0;
    padding: 0;
}

.container {
    max-width: 860px; 
    margin: 0 auto;
    padding: 10px;
}

.nav {
    background-color: grey;
    margin-bottom: 20px;
    padding: 10px;
    text-align: center;
    border-bottom: 2px solid #ddd; 
}

.nav ul {
    display: flex;
    justify-content: center;
    list-style-type: none;
    padding: 0;
}

.nav ul li {
    margin: 0 15px;
}

.nav ul li a {
    text-decoration: none;
    color: black;
    font-size: 18px;
    padding: 10px 15px;
}

.nav ul li a:hover {
    background-color: #f0f0f0; 
}

h1 {
    font-style: italic;
    color: blue;
    margin-bottom: 10px;
}

.trusted {
    text-align: center;
    margin-bottom: 20px;
}

.trusted-banners {
    display: flex;
    justify-content: center;
    list-style-type: none;
    padding: 0;
}

.trusted-banners .banner {
    text-align: center;
    margin: 10px;
    padding: 10px;
}

.footer {
    background-color: #333;
    color: #fff;
    text-align: center;
    padding: 20px;
    position: absolute;
    bottom: 0;
    width: 100%;
    border-top: 2px solid #ddd;
}

.footer .footer-nav {
    list-style-type: none;
    padding: 0;
    margin-top: 15px; 
}

.footer .footer-nav .footer-link {
    display: inline-block;
    margin-right: 20px;
}

.footer .footer-nav .footer-link a {
    color: #fff;
    text-decoration: none;
    font-size: 16px;
    padding: 5px 10px; 
}

.footer .footer-nav .footer-link a:hover {
    background-color: #555; 
}

    </style>
</head>
<body background="/img/math_background.png">
    <header>
        <div class="container">
            <br>
            <br>
            <marquee behavior="scroll" direction="right">
                <h1>WELCOME TO THE MATHEMATICS CHALLENGE SYSTEM</h1>
            </marquee>
            <h4 style="font-weight:200;">International Education Services</h4>
            @if(session('success'))
                <div class="alert alert-success">
                    {{ session('success') }}
                </div>
            @endif
            
            @if($errors->any())
                <div class="alert alert-danger">
                    @foreach($errors->all() as $error)
                        <p>{{ $error }}</p>
                    @endforeach
                </div>
            @endif
        </div>
    </header>
    
    <nav class="nav">
        <div class="container">
            <ul>
                <li><a href="{{ route('page.index', 'login')}}">Admin login</a></li>
                <li><a href="{{ route('challenges.create') }}">Challenges</a></li>
                
                <li><a href="{{ route('upload.create') }}">Questions & answers</a></li>
                <li><a href="{{ route('schools.index') }}">Schools</a></li>
                <li><a href="{{ route('analytics.index') }}">Analysis</a></li>
            </ul>
        </div>
    </nav>
    
    <main>
        <div class="container">
            <section class="trusted">
                <h1>Trusted By</h1>
                <ul class="trusted-banners">
                    <li class="banner">
                        <img src="/img/MOES.png" alt="" width="52" height="52">
                        <span>MOES</span>
                    </li>
                    <li class="banner">
                        <img src="/img/UNEB.jpg" alt="" width="52" height="52">
                        <span>UNEB</span>
                    </li>
                </ul>
            </section>
        </div>
    </main>
    
    <footer class="footer">
        <div class="container">
            <img src="./assets/images/logo.png" width="32" alt="">
            <h1 class="banner">Mathematics</h1>
            <p>Solve the future</p>
            
            <ul class="footer-nav">
                <li class="footer-link active"><a href="{{ route('home') }}">Home</a></li>
                <li class="footer-link"><a href="{{ route('challenges.index') }}">Challenges</a></li>
                <li class="footer-link"><a href="#">Useful links</a></li>
            </ul>
        </div>
    </footer>
</body>
</html>
