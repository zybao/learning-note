# Multiple Features
Linear regression with multiple variables is also known as "multivariate linear regression".

We now introduce notation for equations where we can have any number of input variables.

* x_j^(i) = value of feature j in the ith training example
* x(i) = the input (features) of the ith training example
* m = the number of training examples
* n = the number of features

The multivariable form of the hypothesis function accommodating these multiple features is as follows:

hθ(x) = θ0 + θ1 x1 + θ2 x2 + θ3 x3 + ⋯ + θn xn

In order to develop intuition about this function, we can think about θ0 as the basic price of a house, θ1 as the price per square meter, θ2 as the price per floor, etc. x1 will be the number of square meters in the house, x2 the number of floors, etc.