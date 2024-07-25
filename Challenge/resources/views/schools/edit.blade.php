<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Edit School</title>
    <link href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css" rel="stylesheet">
    <style>
        /* Add the same styles as in the create and index views */
    </style>
</head>
<body>
    <nav class="navbar navbar-expand-lg navbar-dark">
        <!-- Add the same navigation as in the create and index views -->
    </nav>

    <div class="container">
        <div class="row justify-content-center">
            <div class="col-md-8">
                <div class="card">
                    <div class="card-header">Edit School</div>
                    <div class="card-body">
                        @if($errors->any())
                            <div class="alert alert-danger">
                                <ul>
                                    @foreach($errors->all() as $error)
                                        <li>{{ $error }}</li>
                                    @endforeach
                                </ul>
                            </div>
                        @endif
                        <form action="{{ route('schools.update', $school->id) }}" method="POST">
                            @csrf
                            @method('PUT')
                            <div class="form-group">
                                <label for="name">Name</label>
                                <input type="text" class="form-control" id="name" name="name" value="{{ $school->name }}" required>
                            </div>
                            <div class="form-group">
                                <label for="district">district</label>
                                <input type="text" class="form-control" id="district" name="district" value="{{ $school->district }}" required>
                            </div>
                            <div class="form-group">
                                <label for="schoolRegNo">schoolRegNo</label>
                                <input type="text" class="form-control" id="schoolRegNo" name="contact_person" value="{{ $school->schoolRegNo }}" >
                            </div>
                            <div class="form-group">
                                <label for="emailAddress">emailAddress</label>
                                <input type="email" class="form-control" id="email" name="email" value="{{ $school->emailAddress }}" >
                            </div>
                            <div class="form-group">
                                <label for="nameOfRep">nameOfRep</label>
                                <input type="text" class="form-control" id="nameOfRep" name="nameOfRep" value="{{ $school->nameOfRep }}" required>
                            </div>
                            <div class="form-group">
                                <label for="password">password</label>
                                <input type="password" class="form-control" id="password" name="password" required>
                            </div>
                            <button type="submit" class="btn btn-primary">Update School</button>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://code.jquery.com/jquery-3.5.1.slim.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/@popperjs/core@2.5.3/dist/umd/popper.min.js"></script>
    <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/js/bootstrap.min.js"></script>
</body>
</html>